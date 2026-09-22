package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.databinding.ItemLessonBinding
import com.unicoursehub.app.databinding.ItemModuleHeaderBinding

/** Groups a flat lesson list by module name and renders each as an expandable-looking card. */
class ModuleAdapter(
    lessons: List<Lesson>,
    private val onLessonClick: (Lesson) -> Unit,
    private val onMenuClick: ((Lesson, android.view.View) -> Unit)? = null,
    private val onDownloadClick: ((Lesson) -> Unit)? = null
) : RecyclerView.Adapter<ModuleAdapter.ModuleVH>() {

    private val modules: List<Pair<String, List<Lesson>>> =
        lessons.groupBy { it.moduleName }.map { it.key to it.value }
    
    private val expandedPositions = mutableSetOf<Int>()

    inner class ModuleVH(val binding: ItemModuleHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleVH {
        val binding = ItemModuleHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModuleVH(binding)
    }

    override fun onBindViewHolder(holder: ModuleVH, position: Int) {
        val (moduleName, lessons) = modules[position]
        val context = holder.binding.root.context
        
        holder.binding.tvModuleTitle.text = moduleName
        
        val isExpanded = expandedPositions.contains(position)
        holder.binding.lessonContainer.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.ivChevron.rotation = if (isExpanded) 180f else 0f

        holder.binding.headerLayout.setOnClickListener {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
        }

        val container = holder.binding.lessonContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(holder.binding.root.context)
        lessons.forEachIndexed { index, lesson ->
            val lessonBinding = ItemLessonBinding.inflate(inflater, container, false)
            lessonBinding.tvLessonNumber.text = (index + 1).toString()
            lessonBinding.tvTitle.text = lesson.title
            
            val typeLabel = if (lesson.type == "video") "Video" else "Article"
            lessonBinding.tvMeta.text = if (lesson.duration.isNotEmpty() && lesson.duration != "-") {
                "$typeLabel - ${lesson.duration} mins"
            } else {
                typeLabel
            }
            
            lessonBinding.tvCc.visibility = if (lesson.type == "video") android.view.View.VISIBLE else android.view.View.GONE
            lessonBinding.ivWatched.visibility = if (lesson.watched) android.view.View.VISIBLE else android.view.View.GONE
            
            // Lesson Rating display
            if (lesson.rating > 0) {
                lessonBinding.tvLessonRating.visibility = android.view.View.VISIBLE
                val avg = lesson.rating
                val rounded = if (avg % 1 >= 0.5f) avg.toInt() + 0.5f else avg.toInt().toFloat()
                lessonBinding.tvLessonRating.text = String.format(java.util.Locale.getDefault(), "%.1f ★", rounded)
            } else {
                lessonBinding.tvLessonRating.visibility = android.view.View.GONE
            }

            lessonBinding.btnLessonMenu.setOnClickListener { onMenuClick?.invoke(lesson, it) }
            lessonBinding.btnDownload.setOnClickListener { onDownloadClick?.invoke(lesson) }

            if (lesson.status == "processing") {
                lessonBinding.root.setOnClickListener(null)
                lessonBinding.root.alpha = 0.6f
            } else {
                lessonBinding.root.setOnClickListener { onLessonClick(lesson) }
                lessonBinding.root.alpha = 1.0f
            }

            container.addView(lessonBinding.root)
        }
    }

    override fun getItemCount() = modules.size
    override fun getItemId(position: Int): Long = modules[position].first.hashCode().toLong()
}
