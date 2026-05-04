package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R



class WelcomeActivity : AppCompatActivity() {

    private companion object {
        private const val RC_SIGN_IN = 1001
        private const val PREF_NAME = "app_prefs"
        private const val KEY_GUEST_MODE = "guest_mode"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val root = findViewById<View>(R.id.welcome_root)

        root.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }



    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}