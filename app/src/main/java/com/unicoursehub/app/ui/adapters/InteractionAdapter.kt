package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.InstructorInteraction
import com.unicoursehub.app.databinding.ItemInteractionBinding

class InteractionAdapter(
    private val onReply: ((InstructorInteraction) -> Unit)? = null,
    private val onDelete: ((InstructorInteraction) -> Unit)? = null
) : ListAdapter<InstructorInteraction, InteractionAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemInteractionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemInteractionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvType.text = item.type.replace("_", " ").uppercase()
            val bgRes = when (item.type) {
                "comment" -> R.drawable.bg_status_info
                "lesson_rating" -> R.drawable.bg_status_purple
                "instructor_rating" -> R.drawable.bg_status_purple
                else -> R.drawable.bg_status_warning
            }
            tvType.setBackgroundResource(bgRes)
            
            tvUser.text = tvUser.context.getString(R.string.label_from_user, item.userName)
            tvLesson.text = if (item.type == "instructor_rating") "Instructor Profile" else tvLesson.context.getString(R.string.label_lesson, item.lessonTitle)
            tvContent.text = item.text
            tvTimestamp.text = item.timestamp
            
            if (item.rating > 0) {
                ratingBar.visibility = android.view.View.VISIBLE
                // Apply decimal-perfect half-star logic
                val roundedRating = if (item.rating % 1 >= 0.5f) {
                    item.rating.toInt() + 0.5f
                } else {
                    item.rating.toInt().toFloat()
                }
                ratingBar.rating = roundedRating
            } else {
                ratingBar.visibility = android.view.View.GONE
            }
            
            if (onReply != null) {
                btnReply.visibility = android.view.View.VISIBLE
                btnReply.setOnClickListener { onReply.invoke(item) }
            } else {
                btnReply.visibility = android.view.View.GONE
            }

            if (onDelete != null) {
                btnDeleteInteraction.visibility = android.view.View.VISIBLE
                btnDeleteInteraction.setOnClickListener { onDelete.invoke(item) }
            } else {
                btnDeleteInteraction.visibility = android.view.View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InstructorInteraction>() {
        override fun areItemsTheSame(oldItem: InstructorInteraction, newItem: InstructorInteraction) =
            oldItem.timestamp == newItem.timestamp && oldItem.text == newItem.text && oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: InstructorInteraction, newItem: InstructorInteraction) =
            oldItem == newItem
    }
}
