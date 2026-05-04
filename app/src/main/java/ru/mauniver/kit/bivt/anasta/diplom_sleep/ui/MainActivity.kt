package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.mauniver.kit.bivt.anasta.diplom_sleep.adapters.MainPagerAdapter
import ru.mauniver.kit.bivt.anasta.diplom_sleep.services.SleepTrackingService

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.view_pager)
        bottomNav = findViewById(R.id.bottom_navigation)

        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter

        // Свайп - и окно меняется (нумерация с 0)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> bottomNav.selectedItemId = R.id.nav_home
                    1 -> bottomNav.selectedItemId = R.id.nav_diary
                    2 -> bottomNav.selectedItemId = R.id.nav_analytics
                    3 -> bottomNav.selectedItemId = R.id.nav_settings
                }
            }
        })

        // Тут по кнопке окно меняется
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_diary -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_analytics -> {
                    viewPager.currentItem = 2
                    true
                }
                R.id.nav_settings -> {
                    viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }

        // Запуск сервиса отслеживания сна (если разрешения есть)
        checkAndStartService()
    }

    private fun checkAndStartService() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isEmpty()) {
            // Все разрешения уже даны – запускаем сервис
            SleepTrackingService.start(this)
        } else {
            // Запрашиваем недостающие разрешения
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                SleepTrackingService.start(this)
            } else {
                android.widget.Toast.makeText(this, "Для автоопределения сна нужны разрешения", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}