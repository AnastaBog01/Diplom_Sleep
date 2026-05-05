package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogleSignIn: com.google.android.gms.common.SignInButton
    private lateinit var btnGuestMode: Button

    private companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
        private const val PREF_NAME = "app_prefs"
        private const val KEY_GUEST_MODE = "guest_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Инициализация Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnGuestMode = findViewById(R.id.btnGuestMode)

        // Проверяем, не выбран ли гостевой режим ранее
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val isGuest = prefs.getBoolean(KEY_GUEST_MODE, false)
        if (isGuest) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Проверяем, не авторизован ли пользователь уже
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Настройка Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnGuestMode.setOnClickListener {
            prefs.edit().putBoolean(KEY_GUEST_MODE, true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Ошибка входа: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Убираем гостевой режим, если был
                    getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                        .edit().remove(KEY_GUEST_MODE).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Аутентификация не удалась", Toast.LENGTH_SHORT).show()
                }
            }
    }
}