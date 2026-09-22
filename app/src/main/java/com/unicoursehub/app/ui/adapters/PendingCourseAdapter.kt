package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.databinding.ItemPendingCourseBinding

class PendingCourseAdapter(
    private var items: List<Course>,
    private val onApprove: (Course) -> Unit,
    private val onReject: (Course) -> Unit,
    private val onItemClick: (Course) -> Unit,
    private val onMenuClick: ((Course, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<PendingCourseAdapter.VH>() {

    inner class VH(val binding: ItemPendingCourseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPendingCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val course = items[position]
        holder.binding.tvTitle.text = holder.binding.root.context.getString(R.string.title_code_format, course.title, course.code)
        holder.binding.tvInstructor.text = course.instructorName
        
        holder.binding.btnApprove.setOnClickListener { onApprove(course) }
        holder.binding.btnReject.setOnClickListener { onReject(course) }
        holder.binding.btnPendingMenu.setOnClickListener { onMenuClick?.invoke(course, it) }
        holder.binding.root.setOnClickListener { onItemClick(course) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Course>) {
        items = newItems
        notifyDataSetChanged()
    }
}
