package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.databinding.ItemLessonBinding

class DownloadsAdapter(
    private val lessons: List<Lesson>,
    private val onLessonClick: (Lesson) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.LessonVH>() {

    class LessonVH(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonVH {
        val binding = ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LessonVH(binding)
    }

    override fun onBindViewHolder(holder: LessonVH, position: Int) {
        val lesson = lessons[position]
        holder.binding.tvLessonNumber.text = (position + 1).toString()
        holder.binding.tvTitle.text = lesson.title
        holder.binding.tvMeta.text = "Video - ${lesson.duration} mins"
        
        holder.binding.ivWatched.visibility = if (lesson.watched) View.VISIBLE else View.GONE
        holder.binding.tvCc.visibility = View.VISIBLE
        
        // Hide menu/download icons for simple downloads list if not needed
        holder.binding.btnDownload.visibility = View.GONE
        holder.binding.btnLessonMenu.visibility = View.GONE
        
        holder.binding.root.setOnClickListener { onLessonClick(lesson) }
    }

    override fun getItemCount() = lessons.size
}
