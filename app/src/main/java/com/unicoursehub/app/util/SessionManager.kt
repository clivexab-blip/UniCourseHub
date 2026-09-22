package com.unicoursehub.app.util

import android.content.Context

/** Small wrapper around SharedPreferences to remember the logged-in user. */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("unicourse_session", Context.MODE_PRIVATE)

    fun saveSession(userId: Long, role: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1)

    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""

    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}
