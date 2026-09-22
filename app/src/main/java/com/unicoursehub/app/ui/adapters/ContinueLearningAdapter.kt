package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.data.Enrollment
import com.unicoursehub.app.databinding.FragmentContinueLearningItemBinding
import com.unicoursehub.app.util.CourseVisuals

class ContinueLearningAdapter(
    private val onClick: (Course) -> Unit,
    private val onInstructorClick: (Long) -> Unit
) : ListAdapter<Pair<Enrollment, Course>, ContinueLearningAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: FragmentContinueLearningItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = FragmentContinueLearningItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (enrollment, course) = getItem(position)
        with(holder.binding) {
            val context = root.context
            tvTitle.text = course.title
            tvMeta.text = context.getString(
                R.string.course_meta_format,
                course.code,
                course.instructorName.ifEmpty { context.getString(R.string.tba) }
            )
            tvMeta.setOnClickListener { onInstructorClick(course.instructorId) }
            ivIcon.setImageResource(CourseVisuals.iconForCategory(course.category))
            vTile.setBackgroundResource(CourseVisuals.tileDrawable(course.tileColor))
            progressBar.progress = enrollment.progress
            tvProgress.text = context.getString(R.string.completion_format, enrollment.progress)
            root.setOnClickListener { onClick(course) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pair<Enrollment, Course>>() {
        override fun areItemsTheSame(oldItem: Pair<Enrollment, Course>, newItem: Pair<Enrollment, Course>) =
            oldItem.first.id == newItem.first.id
        override fun areContentsTheSame(oldItem: Pair<Enrollment, Course>, newItem: Pair<Enrollment, Course>) =
            oldItem == newItem
    }
}
