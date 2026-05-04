package ru.mauniver.kit.bivt.anasta.diplom_sleep.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.mauniver.kit.bivt.anasta.diplom_sleep.SleepTrackerApplication
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class SleepTrackingService : Service(), SensorEventListener {

    private lateinit var repository: SleepRepository
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // Микрофон: редкие проверки
    private var micHandler: Handler? = null
    private var consecutiveSilentCount = 0
    private var firstSilentTime: Long? = null
    private var isFastCheckMode = false
    private var microphoneActive = false

    // Для усреднения за интервал (1 минута)
    private var motionSum = 0f
    private var motionCount = 0
    private var noiseSum = 0f
    private var noiseCount = 0
    private var currentIntervalStart = System.currentTimeMillis()

    // История сэмплов (храним последние 10 штук)
    private data class Sample(val motion: Float, val noise: Float, val time: Long)
    private val samples = mutableListOf<Sample>()

    // Константы
    private val SAMPLE_INTERVAL_MS = 60_000L           // 1 минута
    private val SLEEP_CONSECUTIVE_COUNT = 3             // 3 тихих минуты подряд → заснул
    private val AWAKE_CONSECUTIVE_COUNT = 2             // 2 активных минуты подряд → проснулся

    // Пороги
    private val MOTION_SLEEP_THRESHOLD = 0.3f
    private val NOISE_SLEEP_THRESHOLD = 0.1f
    private val MOTION_AWAKE_THRESHOLD = 0.5f
    private val NOISE_AWAKE_THRESHOLD = 0.3f

    private var isAsleep = false
    private var asleepStartTime: Long? = null

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepTracking::WakeLock")
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var samplingRunnable: Runnable

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        val app = applicationContext as SleepTrackerApplication
        repository = app.repository
        wakeLock.acquire(10 * 60 * 1000L)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }

        startMicrophoneMonitoring()

        samplingRunnable = object : Runnable {
            override fun run() {
                finishCurrentInterval()
                handler.postDelayed(this, SAMPLE_INTERVAL_MS)
            }
        }
        handler.post(samplingRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        stopMicrophoneMonitoring()
        handler.removeCallbacks(samplingRunnable)
        wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt(x * x + y * y + z * z)
            val motion = Math.abs(acceleration - SensorManager.GRAVITY_EARTH)
            motionSum += motion
            motionCount++
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ==================== МИКРОФОН ====================

    private fun startMicrophoneMonitoring() {
        if (microphoneActive) return
        microphoneActive = true
        scheduleMicrophoneCheck(20 * 60 * 1000L) // первый раз через 20 минут
    }

    private fun scheduleMicrophoneCheck(delay: Long) {
        if (micHandler == null) {
            micHandler = Handler(Looper.getMainLooper())
        }
        micHandler?.postDelayed({
            performMicrophoneCheck()
        }, delay)
    }

    private fun performMicrophoneCheck() {
        if (!microphoneActive) return

        val noise = measureNoiseOnce()
        if (noise == null) {
            scheduleMicrophoneCheck(20 * 60 * 1000L)
            return
        }

        val isSilent = noise < NOISE_SLEEP_THRESHOLD

        if (!isFastCheckMode) {
            // Обычный режим (редкие проверки)
            if (isSilent) {
                // Обнаружена тишина → переходим в режим верификации
                isFastCheckMode = true
                consecutiveSilentCount = 1
                firstSilentTime = System.currentTimeMillis()
                scheduleMicrophoneCheck(3 * 60 * 1000L) // следующая проверка через 3 минуты
            } else {
                scheduleMicrophoneCheck(20 * 60 * 1000L)
            }
        } else {
            // Режим верификации (часто)
            if (isSilent) {
                consecutiveSilentCount++
                if (consecutiveSilentCount >= 3 && !isAsleep) {
                    // Три тихих проверки подряд – фиксируем засыпание
                    isAsleep = true
                    asleepStartTime = firstSilentTime ?: System.currentTimeMillis()
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(asleepStartTime!!))
                    Log.d("SleepTracking", "Заснул в $timeStr (по микрофону)")
                    saveSleepStart(asleepStartTime!!)

                    // После засыпания возвращаемся к редким проверкам
                    isFastCheckMode = false
                    consecutiveSilentCount = 0
                    firstSilentTime = null
                    scheduleMicrophoneCheck(20 * 60 * 1000L)
                    return
                } else {
                    scheduleMicrophoneCheck(3 * 60 * 1000L)
                }
            } else {
                // Шумно – сбрасываем верификацию, возвращаемся к редким проверкам
                Log.d("SleepTracking", "Обнаружен шум, сброс верификации")
                isFastCheckMode = false
                consecutiveSilentCount = 0
                firstSilentTime = null
                scheduleMicrophoneCheck(20 * 60 * 1000L)
            }
        }
    }

    private fun stopMicrophoneMonitoring() {
        microphoneActive = false
        micHandler?.removeCallbacksAndMessages(null)
        micHandler = null
        isFastCheckMode = false
        consecutiveSilentCount = 0
        firstSilentTime = null
        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
    }

    private fun measureNoiseOnce(): Float? {
        var recorder: MediaRecorder? = null
        var tempFile: File? = null
        return try {
            tempFile = File(cacheDir, "temp_audio_${System.currentTimeMillis()}.3gp")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(tempFile?.absolutePath)
                prepare()
                start()
                Thread.sleep(500)
            }
            val amplitude = recorder?.maxAmplitude?.toFloat() ?: 0f
            val normalized = (amplitude / 32768f).coerceIn(0f, 1f)
            noiseSum += normalized
            noiseCount++
            normalized
        } catch (e: Exception) {
            Log.e("SleepTracking", "Microphone measure error", e)
            null
        } finally {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            tempFile?.delete()
        }
    }

    // АНАЛИЗ

    private fun finishCurrentInterval() {
        val now = System.currentTimeMillis()
        val avgMotion = if (motionCount > 0) motionSum / motionCount else 0f
        val avgNoise = if (noiseCount > 0) noiseSum / noiseCount else 0f

        Log.d("SleepTracking", "Интервал: движение=$avgMotion, шум=$avgNoise, спим=$isAsleep")

        if (motionCount > 0 || noiseCount > 0) {
            samples.add(Sample(avgMotion, avgNoise, currentIntervalStart))
            if (samples.size > 10) samples.removeAt(0)
            analyzeSamples()
        }

        val debugStr = "Движение: ${"%.2f".format(avgMotion)} | Шум: ${"%.2f".format(avgNoise)} | Спим: $isAsleep"
        getSharedPreferences("sleep_data", MODE_PRIVATE).edit().putString("debug_log", debugStr).apply()

        motionSum = 0f
        motionCount = 0
        noiseSum = 0f
        noiseCount = 0
        currentIntervalStart = now
    }

    private fun analyzeSamples() {
        if (samples.size < maxOf(SLEEP_CONSECUTIVE_COUNT, AWAKE_CONSECUTIVE_COUNT)) return

        val screenOn = isScreenOn()
        val lastSamples = samples.takeLast(SLEEP_CONSECUTIVE_COUNT)
        val motionAvg = lastSamples.map { it.motion }.average().toFloat()
        val noiseAvg = lastSamples.map { it.noise }.average().toFloat()

        if (!isAsleep) {
            val allSleep = !screenOn && lastSamples.all {
                it.motion < MOTION_SLEEP_THRESHOLD && it.noise < NOISE_SLEEP_THRESHOLD
            }
            if (allSleep) {
                isAsleep = true
                asleepStartTime = lastSamples.first().time
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(asleepStartTime!!))
                Log.d("SleepTracking", "Заснул в $timeStr")
                saveSleepStart(asleepStartTime!!)
            }
        }

        if (isAsleep) {
            val awakeSamples = samples.takeLast(AWAKE_CONSECUTIVE_COUNT)
            val allAwake = awakeSamples.any {
                it.motion > MOTION_AWAKE_THRESHOLD || it.noise > NOISE_AWAKE_THRESHOLD
            } || screenOn

            if (allAwake) {
                isAsleep = false
                val awakeTime = samples.last().time
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(awakeTime))
                Log.d("SleepTracking", "Проснулся в $timeStr")
                saveSleepEnd(awakeTime)
            }
        }
    }

    private fun saveSleepStart(time: Long) {
        val prefs = getSharedPreferences("sleep_data", MODE_PRIVATE)
        val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))
        prefs.edit()
            .putLong("sleep_start", time)
            .putString("last_sleep_start", "Заснул в $date")
            .putString("last_event", "Заснул в $date")
            .apply()
    }

    private fun saveSleepEnd(time: Long) {
        val prefs = getSharedPreferences("sleep_data", MODE_PRIVATE)
        val start = prefs.getLong("sleep_start", 0)
        if (start != 0L) {
            val record = SleepRecord(startTime = start, endTime = time)
            CoroutineScope(Dispatchers.IO).launch {
                repository.insertRecord(record)
            }
            val startDate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(start))
            val endDate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))
            prefs.edit()
                .putString("last_event", "Сон: $startDate – $endDate")
                .putString("last_sleep_end", "Проснулся в $endDate")
                .apply()
        }
        prefs.edit().remove("sleep_start").apply()
    }

    private fun createNotification(): Notification {
        val channelId = "sleep_tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Отслеживание сна",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновое отслеживание движений и шума для определения сна"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Отслеживание сна")
            .setContentText("Приложение анализирует ваш сон")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            powerManager.isScreenOn
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        fun start(context: Context) {
            val intent = Intent(context, SleepTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SleepTrackingService::class.java)
            context.stopService(intent)
        }
    }
}