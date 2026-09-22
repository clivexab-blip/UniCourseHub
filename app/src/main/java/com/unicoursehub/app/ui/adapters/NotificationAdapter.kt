package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.data.AppNotification
import com.unicoursehub.app.databinding.ItemNotificationBinding

class NotificationAdapter : ListAdapter<AppNotification, NotificationAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val notification = getItem(position)
        with(holder.binding) {
            tvTitle.text = notification.title
            tvMessage.text = notification.message
            tvTime.text = notification.timestamp
            vDot.visibility = if (notification.isRead) View.GONE else View.VISIBLE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification) = oldItem == newItem
    }
}
