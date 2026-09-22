package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.ItemShowcaseBinding
import com.unicoursehub.app.util.SessionManager

import com.unicoursehub.app.ui.student.ProjectDetailFragment
import com.unicoursehub.app.ui.main.MainActivity

class ShowcaseAdapter(private var items: List<Project>) : RecyclerView.Adapter<ShowcaseAdapter.VH>() {

    inner class VH(val binding: ItemShowcaseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemShowcaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = items[position]
        with(holder.binding) {
            tvTitle.text = project.title
            tvStack.text = project.category.ifEmpty { project.projectType }
            tvAuthor.text = project.studentName
            tvAuthorEmail.text = project.studentEmail
            tvGitHub.text = project.githubRepo
            tvVideoLabel.text = if (project.videoUrl.isNullOrEmpty()) {
                root.context.getString(com.unicoursehub.app.R.string.label_project_video)
            } else {
                root.context.getString(com.unicoursehub.app.R.string.btn_watch_demo)
            }

            // Setup Video Preview - Navigate to Detail Fragment
            videoPreviewFrame.setOnClickListener {
                (root.context as MainActivity).showFragment(ProjectDetailFragment.newInstance(project.id), addToBackStack = true)
            }
            
            // Apply decimal-perfect half-star logic
            val avgRating = project.rating
            val roundedRating = if (avgRating % 1 >= 0.5f) {
                avgRating.toInt() + 0.5f
            } else {
                avgRating.toInt().toFloat()
            }
            projectRating.rating = roundedRating
            tvRatingCount.text = root.context.getString(com.unicoursehub.app.R.string.rating_count_format, project.ratingCount)
            
            if (!project.badge.isNullOrEmpty()) {
                tvBadge.text = project.badge
                tvBadge.visibility = View.VISIBLE
            } else {
                tvBadge.visibility = View.GONE
            }

            tagContainer.removeAllViews()
            val tags = listOfNotNull(project.category.ifEmpty { null }, project.projectType)
            for (tag in tags) {
                val tv = android.widget.TextView(root.context)
                tv.text = tag
                tv.setTextColor(root.context.getColor(com.unicoursehub.app.R.color.purple_text))
                tv.setBackgroundResource(com.unicoursehub.app.R.drawable.bg_status_purple)
                tv.setPadding(24, 8, 24, 8)
                tv.textSize = 12f
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 8
                tv.layoutParams = params
                tagContainer.addView(tv)
            }

            btnRateProject.setOnClickListener {
                showRateDialog(holder.itemView.context, project, position)
            }
        }
    }

    private fun showRateDialog(context: android.content.Context, project: Project, position: Int) {
        val view = LayoutInflater.from(context).inflate(com.unicoursehub.app.R.layout.dialog_rate_project, null)
        val dialog = AlertDialog.Builder(context, com.unicoursehub.app.R.style.Theme_UniCourseHub_NoBar)
            .setView(view)
            .create()

        val btnClose = view.findViewById<android.widget.ImageView>(com.unicoursehub.app.R.id.btnClose)
        val btnSubmit = view.findViewById<android.view.View>(com.unicoursehub.app.R.id.btnSubmit)
        val rbRating = view.findViewById<android.widget.RatingBar>(com.unicoursehub.app.R.id.dialogRatingBar)
        val rgBadges = view.findViewById<android.widget.RadioGroup>(com.unicoursehub.app.R.id.rgBadges)
        val etComment = view.findViewById<android.widget.EditText>(com.unicoursehub.app.R.id.etProjectComment)

        btnClose.setOnClickListener { dialog.dismiss() }
        
        btnSubmit.setOnClickListener {
            val rating = rbRating.rating
            val comment = etComment.text.toString().trim()
            val checkedId = rgBadges.checkedRadioButtonId
            val badge = if (checkedId != com.unicoursehub.app.R.id.rbNone) {
                view.findViewById<RadioButton>(checkedId)?.text?.toString()
            } else null

            val db = DatabaseHelper.getInstance(context)
            val session = SessionManager(context)
            db.rateProject(project.id, session.getUserId(), rating, badge, if (comment.isEmpty()) null else comment)
            
            // Refresh local item data for immediate feedback
            val updatedProjects = db.getApprovedShowcaseProjects()
            updateData(updatedProjects)
            
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount() = items.size
    override fun getItemId(position: Int): Long = items[position].id

    fun updateData(newItems: List<Project>) {
        items = newItems
        notifyDataSetChanged()
    }
}
