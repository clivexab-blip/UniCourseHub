package com.unicoursehub.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.ActivityRegisterBinding
import com.unicoursehub.app.util.GoogleAuthHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var roles: List<String>

    private val googleSignUpLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showError(getString(
                    R.string.error_google_sign_in, e.message))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper.getInstance(this)
        auth = FirebaseAuth.getInstance()

        roles = listOf(getString(R.string.role_student), getString(R.string.role_instructor), getString(R.string.role_admin))
        val adapter = ArrayAdapter(this, R.layout.item_spinner, roles).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerRole.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.tvGoLogin.setOnClickListener { finish() }
        binding.btnCreateAccount.setOnClickListener { attemptRegister() }
        binding.btnGoogleSignUp.setOnClickListener {
            googleSignUpLauncher.launch(GoogleAuthHelper.client(this).signInIntent)
        }
    }

    private fun attemptRegister() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val university = binding.etUniversity.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()
        val role = roles[binding.spinnerRole.selectedItemPosition].lowercase()

        if (fullName.isEmpty() || email.isEmpty() || university.isEmpty() || studentId.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_fill_fields))
            return
        }
        if (password.length < 6) {
            showError(getString(R.string.msg_password_short))
            return
        }
        if (password != confirm) {
            showError(getString(R.string.msg_password_mismatch))
            return
        }
        if (db.isEmailTaken(email)) {
            showError(getString(R.string.error_email_taken))
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                db.createUserProfile(uid, fullName, email, university, studentId, role)
                Toast.makeText(this, getString(R.string.msg_account_created_login), Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    // Email exists in Firebase. Let's try to sign in to verify the password.
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: return@addOnSuccessListener
                            // Password is correct! Now check/create the local profile.
                            val existing = db.getUserByFirebaseUid(uid)
                            if (existing == null) {
                                db.createUserProfile(uid, fullName, email, university, studentId, role)
                                Toast.makeText(this, getString(R.string.msg_profile_updated_login), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, getString(R.string.msg_account_exists_login), Toast.LENGTH_LONG).show()
                            }
                            finish()
                        }
                        .addOnFailureListener {
                            showError(getString(R.string.error_email_exists_diff_pwd))
                        }
                } else {
                    showError(e.localizedMessage ?: getString(R.string.error_create_account_failed))
                }
            }
    }

    /** Google sign-up shares the same Firebase flow as sign-in; a brand-new account is created automatically. */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                val existing = db.getUserByFirebaseUid(user.uid)
                if (existing != null) {
                    // Already registered — just let them know and send them to log in.
                    Toast.makeText(this, getString(R.string.msg_welcome_back_continue), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    startActivity(
                        Intent(this, CompleteProfileActivity::class.java)
                            .putExtra("uid", user.uid)
                            .putExtra("email", user.email ?: "")
                            .putExtra("displayName", user.displayName ?: "")
                    )
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showError(e.localizedMessage ?: getString(R.string.error_google_sign_in_generic))
            }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
