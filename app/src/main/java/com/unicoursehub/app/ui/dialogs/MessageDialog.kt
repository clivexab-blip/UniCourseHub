package com.unicoursehub.app.ui.dialogs

import android.view.LayoutInflater
import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper

object MessageDialog {
    fun show(context: Context, senderId: Long, receiverId: Long, receiverName: String) {
        val db = DatabaseHelper.getInstance(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_send_message, null)
        val dialog = AlertDialog.Builder(context, R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        val etMsg = dialogView.findViewById<EditText>(R.id.etMessage)
        val tvRecipient = dialogView.findViewById<TextView>(R.id.tvRecipient)
        val btnSend = dialogView.findViewById<android.view.View>(R.id.btnSend)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btnClose)

        tvRecipient.text = "To: $receiverName"

        btnSend.setOnClickListener {
            val text = etMsg.text.toString().trim()
            if (text.isNotEmpty()) {
                db.sendMessage(senderId, receiverId, text)
                Toast.makeText(context, context.getString(R.string.msg_message_sent), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
