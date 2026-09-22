package com.unicoursehub.app.util

import com.unicoursehub.app.R

object CourseVisuals {

    fun tileDrawable(tileColor: String): Int = when (tileColor) {
        "pink" -> R.drawable.bg_icon_tile_pink
        "blue" -> R.drawable.bg_icon_tile_blue
        "orange" -> R.drawable.bg_icon_tile_orange
        else -> R.drawable.bg_icon_tile_purple
    }

    fun iconForCategory(category: String): Int = when (category.lowercase()) {
        "mobile dev" -> R.drawable.ic_video
        "databases" -> R.drawable.ic_database
        "ai/ml" -> R.drawable.ic_brain
        "web dev" -> R.drawable.ic_code
        else -> R.drawable.ic_book
    }

    fun cardBgForLeftBorder(tileColor: String): Int = when (tileColor) {
        "pink" -> R.drawable.bg_card_left_red
        "blue" -> R.drawable.bg_card_left_purple
        "orange" -> R.drawable.bg_card_left_orange
        else -> R.drawable.bg_card_left_purple
    }

    fun statusChipBg(status: String): Int = when (status) {
        "published", "approved", "active", "completed" -> R.drawable.bg_status_success
        "pending" -> R.drawable.bg_status_warning
        "rejected", "suspended" -> R.drawable.bg_status_danger
        "in_progress" -> R.drawable.bg_status_info
        "saved" -> R.drawable.bg_status_purple
        else -> R.drawable.bg_status_info
    }

    fun statusChipTextColor(status: String): Int = when (status) {
        "published", "approved", "active", "completed" -> R.color.success
        "pending" -> R.color.warning
        "rejected", "suspended" -> R.color.danger
        "in_progress" -> R.color.info
        "saved" -> R.color.purple_text
        else -> R.color.info
    }

    fun statusLabelRes(status: String): Int = when (status) {
        "published" -> R.string.status_published
        "approved" -> R.string.status_approved
        "active" -> R.string.status_active
        "completed" -> R.string.status_completed
        "pending" -> R.string.status_pending
        "rejected" -> R.string.status_rejected
        "suspended" -> R.string.status_suspended
        "in_progress" -> R.string.status_in_progress
        "saved" -> R.string.status_saved
        "processing" -> R.string.status_processing
        else -> R.string.status_error
    }

    fun initials(fullName: String): String {
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }
}
