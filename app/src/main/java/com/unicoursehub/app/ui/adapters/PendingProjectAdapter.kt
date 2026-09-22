package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.ItemPendingProjectBinding

class PendingProjectAdapter(
    private var items: List<Project>,
    private val onItemClick: (Project) -> Unit,
    private val onMenuClick: ((Project, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<PendingProjectAdapter.VH>() {

    inner class VH(val binding: ItemPendingProjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPendingProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = items[position]
        val context = holder.binding.root.context
        holder.binding.tvTitle.text = context.getString(R.string.title_student_format, project.title, project.studentName)
        
        // Fix: Pending items should be colored as warning/pending
        holder.binding.root.setBackgroundResource(R.drawable.bg_status_warning)
        
        holder.binding.btnProjectMenu.setOnClickListener { onMenuClick?.invoke(project, it) }
        holder.binding.root.setOnClickListener { onItemClick(project) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Project>) {
        items = newItems
        notifyDataSetChanged()
    }
}
