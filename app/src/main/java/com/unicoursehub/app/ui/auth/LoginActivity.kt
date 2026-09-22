package com.unicoursehub.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.ActivityLoginBinding
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.GoogleAuthHelper
import com.unicoursehub.app.util.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var auth: FirebaseAuth
    private var failedAttempts = 0

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showError("Google sign-in failed: ${e.message}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper.getInstance(this)
        session = SessionManager(this)
        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(GoogleAuthHelper.client(this).signInIntent)
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_enter_email_password))
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                resolveLocalProfile(uid, email, result.user?.displayName ?: "", isGoogle = false)
            }
            .addOnFailureListener {
                failedAttempts++
                if (failedAttempts >= 3) {
                    showError(getString(R.string.error_failed_attempts))
                } else {
                    showError(getString(R.string.error_incorrect_credentials))
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                resolveLocalProfile(user.uid, user.email ?: "", user.displayName ?: "", isGoogle = true)
            }
            .addOnFailureListener { e ->
                showError(e.localizedMessage ?: getString(R.string.error_google_sign_in_generic))
            }
    }

    private fun resolveLocalProfile(uid: String, email: String, displayName: String, isGoogle: Boolean) {
        var profile = db.getUserByFirebaseUid(uid)

        // Permanent fix: Check SQLite for role based on email if UID doesn't match
        if (profile == null) {
            val existingByEmail = db.getAllUsers().find { it.email.equals(email, ignoreCase = true) }
            if (existingByEmail != null) {
                db.linkFirebaseUidToEmail(email, uid)
                profile = db.getUserByFirebaseUid(uid)
            }
        }

        // AUTO-REPAIR: If user is authenticated in Firebase but missing from SQLite
        // (This happens if the DB was wiped or this is a new SSO user)
        if (profile == null) {
            val name = displayName.ifBlank { email.substringBefore("@") }
            val autoRole = when {
                email.contains("admin", ignoreCase = true) -> "admin"
                email.contains("instructor", ignoreCase = true) || email.contains("teacher", ignoreCase = true) -> "instructor"
                else -> "student"
            }
            
            // Create the missing profile so the user can actually use the app
            val newId = db.createUserProfile(uid, name, email, "University", "PENDING", autoRole)
            profile = db.getUserById(newId)
        }

        if (profile == null) {
            auth.signOut()
            showError(getString(R.string.error_incorrect_credentials))
            return
        }

        if (profile.status == "suspended") {
            auth.signOut()
            showError(getString(R.string.error_account_suspended))
            return
        }

        failedAttempts = 0
        session.saveSession(profile.id, profile.role)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
