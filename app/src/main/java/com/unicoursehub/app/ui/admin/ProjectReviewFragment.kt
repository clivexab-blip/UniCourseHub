package com.unicoursehub.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.FragmentProjectReviewBinding
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.FileViewerHelper

class ProjectReviewFragment : Fragment() {

    private var _binding: FragmentProjectReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private var projectId: Long = -1
    private var player: ExoPlayer? = null

    companion object {
        private const val ARG_PROJECT_ID = "project_id"
        fun newInstance(projectId: Long) = ProjectReviewFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PROJECT_ID, projectId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProjectReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        val session = com.unicoursehub.app.util.SessionManager(requireContext())
        val role = session.getRole()
        
        projectId = arguments?.getLong(ARG_PROJECT_ID) ?: -1

        setupTabs()

        val project = db.getAllProjects().find { it.id == projectId } ?: return

        binding.tvProjectTitle.text = project.title
        binding.tvStudentName.text = project.studentName
        binding.tvDescription.text = project.description

        // Role-based visibility and actions (Matching Course Review Design)
        if (role == "admin") {
            binding.btnProjectActions.visibility = View.VISIBLE
            binding.btnProjectActions.setOnClickListener { showAdminProjectMenu(it) }
            binding.reviewActions.visibility = View.GONE
            binding.btnRateProject.visibility = View.GONE
        } else if (role == "instructor") {
            binding.btnProjectActions.visibility = View.GONE
            binding.reviewActions.visibility = View.VISIBLE
            binding.btnRateProject.visibility = View.VISIBLE
            binding.btnRateProject.setOnClickListener { showRateDialog(project) }
        }

        if (!project.linkedDocPath.isNullOrEmpty()) {
            binding.btnViewDocument.visibility = View.VISIBLE
            binding.btnViewDocument.setOnClickListener {
                com.unicoursehub.app.util.FileViewerHelper.viewDocument(requireContext(), project.linkedDocPath!!)
            }
        }

        if (!project.githubRepo.isNullOrEmpty()) {
            binding.btnOpenGithub.visibility = View.VISIBLE
            binding.btnOpenGithub.setOnClickListener {
                com.unicoursehub.app.util.FileViewerHelper.openUrl(requireContext(), project.githubRepo)
            }
        }

        initializePlayer(project)
    }

    private fun showAdminProjectMenu(view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Approve Project")
        popup.menu.add("Reject Project")
        popup.menu.add("Delete Project")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Approve Project" -> approveProject()
                "Reject Project" -> rejectProject()
                "Delete Project" -> deleteProject()
            }
            true
        }
        popup.show()
    }

    private fun showRateDialog(project: Project) {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(com.unicoursehub.app.R.layout.dialog_rate_project, null)
        val dialog = AlertDialog.Builder(context, R.style.Theme_UniCourseHub_NoBar)
            .setView(view)
            .create()

        val btnClose = view.findViewById<android.view.View>(com.unicoursehub.app.R.id.btnClose)
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
                view.findViewById<android.widget.RadioButton>(checkedId)?.text?.toString()
            } else null

            val session = com.unicoursehub.app.util.SessionManager(context)
            db.rateProject(project.id, session.getUserId(), rating, badge, if (comment.isEmpty()) null else comment)
            
            Toast.makeText(context, R.string.msg_comment_success, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun setupTabs() {
        binding.tabDetails.setOnClickListener {
            binding.scrollDetails.visibility = View.VISIBLE
            binding.scrollLinks.visibility = View.GONE
            binding.tabDetails.setTextColor(requireContext().getColor(R.color.white))
            binding.tabDetails.setBackgroundResource(R.drawable.bg_tab_indicator)
            binding.tabLinks.setTextColor(requireContext().getColor(R.color.text_hint))
            binding.tabLinks.background = null
        }
        binding.tabLinks.setOnClickListener {
            binding.scrollDetails.visibility = View.GONE
            binding.scrollLinks.visibility = View.VISIBLE
            binding.tabLinks.setTextColor(requireContext().getColor(R.color.white))
            binding.tabLinks.setBackgroundResource(R.drawable.bg_tab_indicator)
            binding.tabDetails.setTextColor(requireContext().getColor(R.color.text_hint))
            binding.tabDetails.background = null
        }
    }

    private fun approveProject() {
        db.setProjectStatus(projectId, "approved")
        Toast.makeText(requireContext(), R.string.msg_project_approved, Toast.LENGTH_SHORT).show()
        
        // Simulation: Notify Student via Cloud Push
        val project = db.getAllProjects().find { it.id == projectId }
        project?.let {
            com.unicoursehub.app.util.PushNotificationSender.sendStatusNotification(
                requireContext(),
                it.studentId,
                "Project Approved! 🏆",
                "Great job! Your project '${it.title}' is now in the Showcase."
            )
        }
        
        binding.reviewActions.visibility = View.GONE
        binding.btnProjectActions.visibility = View.GONE
    }

    private fun rejectProject() {
        db.setProjectStatus(projectId, "rejected")
        Toast.makeText(requireContext(), R.string.msg_project_rejected, Toast.LENGTH_SHORT).show()
        
        // Simulation: Notify Student via Cloud Push
        val project = db.getAllProjects().find { it.id == projectId }
        project?.let {
            com.unicoursehub.app.util.PushNotificationSender.sendStatusNotification(
                requireContext(),
                it.studentId,
                "Project Feedback ⚠️",
                "Your project '${it.title}' needs some adjustments. Check dashboard."
            )
        }
        
        binding.reviewActions.visibility = View.GONE
        binding.btnProjectActions.visibility = View.GONE
    }

    private fun deleteProject() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
        dialogView.findViewById<TextView>(R.id.tvMessage).text = "Are you sure you want to delete this project?"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_close_circle)
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setColorFilter(requireContext().getColor(R.color.danger))
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
            text = getString(R.string.btn_delete)
            setOnClickListener {
                db.deleteProject(projectId)
                Toast.makeText(requireContext(), R.string.msg_account_deleted, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun initializePlayer(project: Project) {
        player?.release()
        player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            
            val videoUri = project.localVideoPath ?: project.videoUrl
            if (videoUri != null) {
                val uri = if (videoUri.startsWith("/")) "file://$videoUri" else videoUri
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }

        setupCustomController()
    }

    private fun setupCustomController() {
        val controller = binding.playerView.findViewById<View>(R.id.layoutTop)?.parent as? View ?: return
        
        controller.findViewById<View>(R.id.btnRewind15)?.setOnClickListener {
            player?.let { p -> p.seekTo(maxOf(0, p.currentPosition - 15000)) }
        }
        controller.findViewById<View>(R.id.btnForward15)?.setOnClickListener {
            player?.let { p -> p.seekTo(minOf(p.duration, p.currentPosition + 15000)) }
        }
        controller.findViewById<View>(R.id.exo_back)?.setOnClickListener {
            if (requireActivity().requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                toggleFullscreen()
            } else {
                parentFragmentManager.popBackStack()
            }
        }
        controller.findViewById<View>(R.id.btnFullscreen)?.setOnClickListener {
            toggleFullscreen()
        }
        controller.findViewById<android.widget.TextView>(R.id.btnPlaybackSpeed)?.setOnClickListener { view ->
            showSpeedMenu(view as android.widget.TextView)
        }
    }

    private fun toggleFullscreen() {
        val activity = requireActivity() as MainActivity
        val decorView = activity.window.decorView
        
        if (activity.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            binding.playerContainer.layoutParams.height = (220 * resources.displayMetrics.density).toInt()
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            activity.setToolbarVisibility(true)
            activity.setBottomNavVisibility(true)
        } else {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            binding.playerContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            activity.setToolbarVisibility(false)
            activity.setBottomNavVisibility(false)
        }
    }

    private fun showSpeedMenu(anchor: android.widget.TextView) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
        speeds.forEach { speed -> popup.menu.add("${speed}X") }
        popup.setOnMenuItemClickListener { item ->
            val speed = item.title.toString().removeSuffix("X").toFloat()
            player?.setPlaybackSpeed(speed)
            anchor.text = item.title
            true
        }
        popup.show()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
}
