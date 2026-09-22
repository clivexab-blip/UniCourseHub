package com.unicoursehub.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.ActivityCompleteProfileBinding
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

/**
 * Shown once, right after a brand-new Firebase account (email/password or Google) signs in
 * for the first time. Collects the role/university/student-id that Firebase itself has no
 * concept of, then creates the matching profile row in the local SQLite database.
 */
class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteProfileBinding
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private val roles = listOf("Student", "Instructor", "Admin")

    private lateinit var uid: String
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper.getInstance(this)
        session = SessionManager(this)

        uid = intent.getStringExtra("uid") ?: run { finish(); return }
        email = intent.getStringExtra("email") ?: ""
        val displayName = intent.getStringExtra("displayName") ?: ""

        binding.etFullName.setText(displayName)
        binding.tvEmail.text = email

        val adapter = ArrayAdapter(this, R.layout.item_spinner, roles).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerRole.adapter = adapter

        binding.btnContinue.setOnClickListener { submit() }

        binding.tvGoLogin.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.tvGoRegister.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun submit() {
        val fullName = binding.etFullName.text.toString().trim()
        val university = binding.etUniversity.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val role = roles[binding.spinnerRole.selectedItemPosition].lowercase()

        if (fullName.isEmpty() || university.isEmpty() || studentId.isEmpty()) {
            binding.tvError.text = getString(R.string.error_fill_fields)
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val newId = db.createUserProfile(uid, fullName, email, university, studentId, role)
        session.saveSession(newId, role)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
