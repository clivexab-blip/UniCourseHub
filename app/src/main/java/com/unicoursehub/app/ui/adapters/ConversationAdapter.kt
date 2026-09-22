package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.data.ChatMessage
import com.unicoursehub.app.data.User
import com.unicoursehub.app.databinding.ItemMessageBinding

class ConversationAdapter(private val onClick: (User) -> Unit) : ListAdapter<Pair<User, ChatMessage>, ConversationAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (user, lastMsg) = getItem(position)
        with(holder.binding) {
            tvSender.text = user.fullName
            tvMessageText.text = lastMsg.text
            tvTimestamp.text = lastMsg.timestamp
            root.setOnClickListener { onClick(user) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pair<User, ChatMessage>>() {
        override fun areItemsTheSame(oldItem: Pair<User, ChatMessage>, newItem: Pair<User, ChatMessage>) = oldItem.first.id == newItem.first.id
        override fun areContentsTheSame(oldItem: Pair<User, ChatMessage>, newItem: Pair<User, ChatMessage>) = oldItem.second.id == newItem.second.id
    }
}
