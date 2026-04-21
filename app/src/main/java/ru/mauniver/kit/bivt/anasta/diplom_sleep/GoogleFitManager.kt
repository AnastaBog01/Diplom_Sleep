//import android.app.Activity
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.android.gms.fitness.Fitness
//import com.google.android.gms.fitness.FitnessOptions
//import com.google.android.gms.fitness.data.DataType
//import com.google.android.gms.fitness.data.Field
//import com.google.android.gms.fitness.request.SessionReadRequest
//import com.google.android.gms.tasks.Tasks
//import kotlinx.coroutines.tasks.await
//import java.util.concurrent.TimeUnit
//
//class GoogleFitManager(private val activity: Activity) {
//
//    // FitnessOptions для запроса доступа к данным (шаги, сон)
//    private val fitnessOptions = FitnessOptions.builder()
//        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
//        .build()
//
//    // Проверка, есть ли уже разрешения
//    fun hasPermissions(): Boolean {
//        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return false
//        return GoogleSignIn.hasPermissions(account, fitnessOptions)
//    }
//
//    // Получение Intent для запроса разрешений (не путать с GoogleSignInOptions!)
//    fun getSignInIntent() = GoogleSignIn.getClient(activity, fitnessOptions).signInIntent
//
//    // Получение шагов за последние 7 дней
//    suspend fun getTotalStepsForLastDays(days: Int = 7): Long? {
//        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return null
//        val endTime = System.currentTimeMillis()
//        val startTime = endTime - TimeUnit.DAYS.toMillis(days.toLong())
//
//        val response = Tasks.await(
//            Fitness.getHistoryClient(activity, account)
//                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
//        )
//        return response?.dataPoints?.firstOrNull()
//            ?.getValue(Field.FIELD_STEPS)?.asInt()?.toLong() ?: 0L
//    }
//
//    // Получение последней сессии сна
//    suspend fun getLastSleepSession(daysBack: Int = 7): Pair<Long, Long>? {
//        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return null
//        val endTime = System.currentTimeMillis()
//        val startTime = endTime - TimeUnit.DAYS.toMillis(daysBack.toLong())
//
//        val request = SessionReadRequest.Builder()
//            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_SLEEP_SEGMENT)
//            .build()
//
//        val response = Tasks.await(
//            Fitness.getSessionsClient(activity, account).readSession(request)
//        )
//
//        // Сессии сна обычно имеют activity = "sleep"
//        val sleepSessions = response.sessions.filter { it.activity == "sleep" }
//        if (sleepSessions.isEmpty()) return null
//
//        val latest = sleepSessions.maxByOrNull { it.startTimeMillis }
//        return latest?.let { Pair(it.startTimeMillis, it.endTimeMillis) }
//    }
//}