package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.ChatMessage
import com.unicoursehub.app.databinding.ItemMessageBinding

class MessageAdapter : ListAdapter<ChatMessage, MessageAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = getItem(position)
        with(holder.binding) {
            tvMessageText.text = message.text
            tvTimestamp.text = message.timestamp
            // Use localized sender labels
            tvSender.text = if (message.senderId == 3L) {
                root.context.getString(R.string.label_sender_instructor, "Sphumelele")
            } else {
                root.context.getString(R.string.label_sender_system)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}
