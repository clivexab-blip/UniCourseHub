package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.ItemProjectStudentBinding
import com.unicoursehub.app.util.CourseVisuals

class ProjectStudentAdapter(
    private var items: List<Project>,
    private val onClick: (Project) -> Unit = {},
    private val onMenuClick: (Project, android.view.View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ProjectStudentAdapter.VH>() {

    inner class VH(val binding: ItemProjectStudentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProjectStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = items[position]
        with(holder.binding) {
            tvTitle.text = project.title
            tvMeta.text = root.context.getString(R.string.submitted_date_format, project.submittedDate)
            tvCategory.text = project.category.ifEmpty { project.projectType }
            tvStatus.text = root.context.getString(CourseVisuals.statusLabelRes(project.status))
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(project.status))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(project.status)))
            
            ivIcon.setImageResource(CourseVisuals.iconForCategory(project.category))
            vTile.setBackgroundResource(CourseVisuals.tileDrawable(if (project.category == "Mobile Dev") "pink" else "orange"))

            btnProjectMenu.setOnClickListener { onMenuClick(project, it) }
            root.setOnClickListener { onClick(project) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Project>) {
        items = newItems
        notifyDataSetChanged()
    }
}
