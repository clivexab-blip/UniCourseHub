package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.data.User
import com.unicoursehub.app.databinding.ItemCourseInstructorBinding
import com.unicoursehub.app.databinding.ItemCourseOverviewBinding
import com.unicoursehub.app.databinding.ItemProjectReviewBinding
import com.unicoursehub.app.databinding.ItemStudentRowBinding
import com.unicoursehub.app.util.CourseVisuals

class CourseInstructorAdapter(
    private var items: List<Course>,
    private val onClick: (Course) -> Unit,
    private val onMenuClick: ((Course, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<CourseInstructorAdapter.VH>() {

    inner class VH(val binding: ItemCourseInstructorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemCourseInstructorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val course = items[position]
        with(holder.binding) {
            val context = root.context
            tvTitle.text = course.title
            tvMeta.text = context.getString(
                R.string.course_meta_format,
                course.code,
                context.getString(R.string.students_count_format, course.studentsCount)
            )
            ivIcon.setImageResource(CourseVisuals.iconForCategory(course.category))
            vTile.setBackgroundResource(CourseVisuals.tileDrawable(course.tileColor))
            tvStatus.text = context.getString(CourseVisuals.statusLabelRes(course.status))
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(course.status))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(course.status)))
            
            btnCourseMenu.setOnClickListener { onMenuClick?.invoke(course, it) }
            root.setOnClickListener { onClick(course) }
        }
    }

    override fun getItemCount() = items.size
    fun updateData(newItems: List<Course>) { items = newItems; notifyDataSetChanged() }
}

class CourseOverviewAdapter(
    private var items: List<Course>,
    private val onClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseOverviewAdapter.VH>() {

    inner class VH(val binding: ItemCourseOverviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemCourseOverviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val course = items[position]
        with(holder.binding) {
            val context = root.context
            tvTitle.text = course.title
            tvMeta.text = context.getString(
                R.string.course_meta_format,
                course.code,
                context.getString(R.string.students_count_format, course.studentsCount)
            )
            ivIcon.setImageResource(CourseVisuals.iconForCategory(course.category))
            vTile.setBackgroundResource(CourseVisuals.tileDrawable(course.tileColor))
            progressBar.progress = course.completionRate
            tvCompletion.text = context.getString(R.string.completion_rate_format, course.completionRate)
            root.setOnClickListener { onClick(course) }
        }
    }

    override fun getItemCount() = items.size
    fun updateData(newItems: List<Course>) { items = newItems; notifyDataSetChanged() }
}

class StudentRowAdapter(
    private var items: List<Pair<User, Pair<String, Int>>>,
    private val onChatClick: (User) -> Unit
) : RecyclerView.Adapter<StudentRowAdapter.VH>() {
    // Pair<User, Pair<courseTitle, progress>>

    inner class VH(val binding: ItemStudentRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemStudentRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (user, courseProgress) = items[position]
        val (courseTitle, progress) = courseProgress
        with(holder.binding) {
            val context = root.context
            tvAvatar.text = CourseVisuals.initials(user.fullName)
            tvName.text = user.fullName
            tvMeta.text = context.getString(R.string.course_meta_format, courseTitle, context.getString(R.string.progress_format, progress))
            val statusKey = if (progress < 50) "pending" else "active"
            tvStatus.text = if (progress < 50) context.getString(R.string.struggling) else context.getString(R.string.active)
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(statusKey))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(statusKey)))
            btnChat.setOnClickListener { onChatClick(user) }
        }
    }

    override fun getItemCount() = items.size
    override fun getItemId(position: Int): Long = items[position].first.id

    fun updateData(newItems: List<Pair<User, Pair<String, Int>>>) { items = newItems; notifyDataSetChanged() }
}

class ProjectReviewAdapter(
    private var items: List<Project>,
    private val onPreview: (Project) -> Unit,
    private val onMenuClick: ((Project, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<ProjectReviewAdapter.VH>() {

    private var activePlayer: ExoPlayer? = null

    inner class VH(val binding: ItemProjectReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemProjectReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = items[position]
        with(holder.binding) {
            val context = root.context
            tvTitle.text = project.title
            tvMeta.text = context.getString(R.string.course_meta_format, project.studentName, context.getString(R.string.submitted_date_format, project.submittedDate))
            tvStatus.text = context.getString(CourseVisuals.statusLabelRes(project.status))
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(project.status))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(project.status)))
            
            ivIcon.setImageResource(CourseVisuals.iconForCategory(project.category))
            vTile.setBackgroundResource(CourseVisuals.tileDrawable(if (project.category == "Mobile Dev") "pink" else "orange"))

            btnPreview.setOnClickListener { onPreview(project) }
            btnProjectMenu.setOnClickListener { onMenuClick?.invoke(project, it) }

            // Internal video playback check
            if (!project.localVideoPath.isNullOrEmpty() || !project.videoUrl.isNullOrEmpty()) {
                playerView.visibility = android.view.View.VISIBLE
            } else {
                playerView.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount() = items.size
    fun updateData(newItems: List<Project>) { items = newItems; notifyDataSetChanged() }
}
