package com.unicoursehub.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.ui.auth.CompleteProfileActivity
import com.unicoursehub.app.ui.auth.LoginActivity
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply persisted theme
        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        if (sharedPref.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_splash)

        val db = DatabaseHelper.getInstance(this)
        db.readableDatabase // warm up / creates + seeds the SQLite database on first launch

        Handler(Looper.getMainLooper()).postDelayed({
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val intent = if (firebaseUser != null) {
                val profile = db.getUserByFirebaseUid(firebaseUser.uid)
                if (profile != null) {
                    SessionManager(this).saveSession(profile.id, profile.role)
                    Intent(this, MainActivity::class.java)
                } else {
                    // Signed into Firebase but never finished picking a role
                    Intent(this, CompleteProfileActivity::class.java)
                        .putExtra("uid", firebaseUser.uid)
                        .putExtra("email", firebaseUser.email ?: "")
                        .putExtra("displayName", firebaseUser.displayName ?: "")
                }
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 1400)
    }
}
