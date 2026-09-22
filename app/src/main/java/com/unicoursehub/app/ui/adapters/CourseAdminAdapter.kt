package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.databinding.ItemCourseAdminBinding
import com.unicoursehub.app.util.CourseVisuals

class CourseAdminAdapter(
    private var items: List<Course>,
    private val actionLabel: (Course) -> Int = { R.string.btn_edit },
    private val onAction: (Course) -> Unit,
    private val onMenuClick: ((Course, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<CourseAdminAdapter.VH>() {

    inner class VH(val binding: ItemCourseAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCourseAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val course = items[position]
        with(holder.binding) {
            tvTitle.text = course.title
            tvMeta.text = root.context.getString(R.string.course_meta_format, course.code, course.instructorName.ifEmpty { root.context.getString(R.string.unassigned) })
            ivIcon.setImageResource(CourseVisuals.iconForCategory(course.category))
            ivIcon.setBackgroundResource(CourseVisuals.tileDrawable(course.tileColor))
            tvStatus.text = root.context.getString(CourseVisuals.statusLabelRes(course.status))
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(course.status))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(course.status)))
            btnAction.text = root.context.getString(actionLabel(course))
            btnAction.setOnClickListener { onAction(course) }
            btnCourseMenu.setOnClickListener { onMenuClick?.invoke(course, it) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Course>) {
        items = newItems
        notifyDataSetChanged()
    }
}
