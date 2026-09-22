package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.databinding.ItemReferenceBinding

class ReferenceAdapter(
    private var items: List<Triple<Lesson, String, String>>,
    private val onPlayClick: (Lesson) -> Unit,
    private val onSaveNotes: (Lesson, String) -> Unit,
    private val onDeleteClick: (Lesson) -> Unit
) : RecyclerView.Adapter<ReferenceAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemReferenceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReferenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (lesson, notes, aiSummary) = items[position]
        holder.binding.tvLessonTitle.text = lesson.title
        holder.binding.tvModuleName.text = lesson.moduleName
        holder.binding.etNotes.setText(notes)

        if (aiSummary.isNotEmpty()) {
            holder.binding.layoutAiSummary.visibility = android.view.View.VISIBLE
            holder.binding.tvAiSummary.text = aiSummary
        } else {
            holder.binding.layoutAiSummary.visibility = android.view.View.GONE
        }

        holder.binding.btnPlay.setOnClickListener { onPlayClick(lesson) }
        holder.binding.btnSaveNotes.setOnClickListener {
            onSaveNotes(lesson, holder.binding.etNotes.text.toString())
        }
        holder.binding.btnDeleteReference.setOnClickListener {
            onDeleteClick(lesson)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Triple<Lesson, String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
