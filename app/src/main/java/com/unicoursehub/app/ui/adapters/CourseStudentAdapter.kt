package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.databinding.ItemCourseStudentBinding
import com.unicoursehub.app.util.CourseVisuals

/** Data holder pairing a course with the enrollment status label to show (may be "Not Enrolled"). */
data class StudentCourseRow(val course: Course, val statusLabel: Int, val statusKey: String)

class CourseStudentAdapter(
    private val onClick: (StudentCourseRow) -> Unit,
    private val onInstructorClick: (Long) -> Unit
) : ListAdapter<StudentCourseRow, CourseStudentAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemCourseStudentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCourseStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getItem(position)
        with(holder.binding) {
            tvTitle.text = row.course.title
            tvMeta.text = root.context.getString(R.string.course_meta_format, row.course.code, row.course.instructorName.ifEmpty { root.context.getString(R.string.tba) })
            tvMeta.setOnClickListener { onInstructorClick(row.course.instructorId) }
            ivIcon.setImageResource(CourseVisuals.iconForCategory(row.course.category))
            vTile.setBackgroundResource(CourseVisuals.tileDrawable(row.course.tileColor))
            tvStatus.text = root.context.getString(row.statusLabel)
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(row.statusKey))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(row.statusKey)))
            root.setOnClickListener { onClick(row) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StudentCourseRow>() {
        override fun areItemsTheSame(oldItem: StudentCourseRow, newItem: StudentCourseRow) = 
            oldItem.course.id == newItem.course.id
        override fun areContentsTheSame(oldItem: StudentCourseRow, newItem: StudentCourseRow) = 
            oldItem == newItem
    }
}
