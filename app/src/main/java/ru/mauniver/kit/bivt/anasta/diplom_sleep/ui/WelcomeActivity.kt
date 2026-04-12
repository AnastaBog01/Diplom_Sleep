package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.sleeptracker.R


class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        findViewById<android.view.View>(android.R.id.content).setOnClickListener { //Находим корневой элемент экрана (белый лист) и при нажатии выолняется...
            startActivity(Intent(this, MainActivity::class.java)) //Вот это и выполняетя (идет на главный экран)
            finish()
        }
    }
}