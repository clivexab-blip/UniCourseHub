package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.unicoursehub.app.data.Comment
import com.unicoursehub.app.databinding.ItemCommentBinding

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentVH>(DiffCallback()) {

    inner class CommentVH(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentVH(binding)
    }

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        val comment = getItem(position)
        holder.binding.tvUserName.text = comment.userName
        holder.binding.tvTimestamp.text = comment.timestamp
        holder.binding.tvCommentText.text = comment.text
    }

    class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}
