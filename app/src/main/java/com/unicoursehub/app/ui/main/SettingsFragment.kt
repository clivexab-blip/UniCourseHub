package com.unicoursehub.app.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.unicoursehub.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val languages = listOf("English", "Afrikaans", "Zulu", "Xhosa", "French")
    private val languageCodes = listOf("en", "af", "zu", "xh", "fr")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSwitch()
        setupLanguageSpinner()
        setupPlaceholderLinks()
    }

    private fun setupThemeSwitch() {
        val sharedPref = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        
        binding.switchDarkMode.isChecked = isDarkMode
        
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)
        }
    }

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = adapter

        // Get current app locale
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLocale = if (currentLocales.isEmpty) "en" else currentLocales.get(0)?.language ?: "en"
        val currentIndex = languageCodes.indexOf(currentLocale).coerceAtLeast(0)
        binding.spinnerLanguage.setSelection(currentIndex, false)

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCode = languageCodes[position]
                if (selectedCode != currentLocale) {
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(selectedCode)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPlaceholderLinks() {
        binding.btnChangePassword.setOnClickListener {
            val isVisible = binding.layoutChangePassword.visibility == View.VISIBLE
            binding.layoutChangePassword.visibility = if (isVisible) View.GONE else View.VISIBLE
        }

        binding.btnUpdatePassword.setOnClickListener {
            updateUserPassword()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_opening_privacy, Toast.LENGTH_SHORT).show()
        }
        binding.btnDeleteAccount.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(com.unicoursehub.app.R.layout.dialog_confirm_action, null)
            val dialog = AlertDialog.Builder(requireContext(), com.unicoursehub.app.R.style.Theme_UniCourseHub_NoBar)
                .setView(dialogView)
                .create()
            
            dialogView.findViewById<android.widget.TextView>(com.unicoursehub.app.R.id.tvTitle).text = getString(com.unicoursehub.app.R.string.settings_delete)
            dialogView.findViewById<android.widget.TextView>(com.unicoursehub.app.R.id.tvMessage).text = getString(com.unicoursehub.app.R.string.msg_confirm_delete_account)
            dialogView.findViewById<android.widget.ImageView>(com.unicoursehub.app.R.id.ivIcon).apply {
                setImageResource(com.unicoursehub.app.R.drawable.ic_close_circle)
                setColorFilter(requireContext().getColor(com.unicoursehub.app.R.color.danger))
            }
            
            dialogView.findViewById<View>(com.unicoursehub.app.R.id.btnCancel).setOnClickListener { dialog.dismiss() }
            dialogView.findViewById<android.widget.Button>(com.unicoursehub.app.R.id.btnConfirm).apply {
                text = getString(com.unicoursehub.app.R.string.btn_delete)
                setOnClickListener {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val session = com.unicoursehub.app.util.SessionManager(requireContext())
                    val db = com.unicoursehub.app.data.DatabaseHelper.getInstance(requireContext())
                    val userId = session.getUserId()
                    
                    user?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.deleteUser(userId)
                            Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_account_deleted, Toast.LENGTH_LONG).show()
                            session.logout()
                            startActivity(Intent(requireContext(), com.unicoursehub.app.ui.auth.LoginActivity::class.java))
                            requireActivity().finish()
                        } else {
                            Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_auth_required_delete, Toast.LENGTH_LONG).show()
                        }
                    }
                    dialog.dismiss()
                }
            }
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
    }

    private fun updateUserPassword() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_password_short, Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_password_mismatch, Toast.LENGTH_SHORT).show()
            return
        }

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_password_updated, Toast.LENGTH_SHORT).show()
                        binding.etNewPassword.setText("")
                        binding.etConfirmPassword.setText("")
                        binding.layoutChangePassword.visibility = View.GONE
                    } else {
                        Toast.makeText(requireContext(), com.unicoursehub.app.R.string.msg_password_update_fail, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
