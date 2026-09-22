package com.unicoursehub.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileViewerHelper {

    fun viewDocument(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, context.getString(com.unicoursehub.app.R.string.error_file_not_found), Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(context, "com.unicoursehub.app.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(com.unicoursehub.app.R.string.error_no_app_for_file), Toast.LENGTH_LONG).show()
        }
    }

    fun viewVideo(context: Context, filePath: String) = viewDocument(context, filePath)

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(com.unicoursehub.app.R.string.error_invalid_link), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(url: String): String {
        return when {
            url.endsWith(".pdf") -> "application/pdf"
            url.endsWith(".doc") -> "application/msword"
            url.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            url.endsWith(".mp4") -> "video/mp4"
            else -> "*/*"
        }
    }
}
