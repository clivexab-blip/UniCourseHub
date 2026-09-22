package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.InstructorInteraction
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.FragmentProjectDetailBinding
import com.unicoursehub.app.util.FileViewerHelper
import com.unicoursehub.app.util.SessionManager

class ProjectDetailFragment : Fragment() {

    private var _binding: FragmentProjectDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private var projectId: Long = -1
    private var player: ExoPlayer? = null

    companion object {
        private const val ARG_PROJECT_ID = "project_id"
        fun newInstance(projectId: Long) = ProjectDetailFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PROJECT_ID, projectId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProjectDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        projectId = arguments?.getLong(ARG_PROJECT_ID) ?: -1

        setupTabs()

        val project = db.getAllProjects().find { it.id == projectId } ?: return

        binding.tvProjectTitle.text = project.title
        binding.tvStudentName.text = project.studentName
        binding.tvDescription.text = project.description

        if (!project.linkedDocPath.isNullOrEmpty()) {
            binding.btnViewDocument.visibility = View.VISIBLE
            binding.btnViewDocument.setOnClickListener {
                FileViewerHelper.viewDocument(requireContext(), project.linkedDocPath!!)
            }
        }

        if (!project.githubRepo.isNullOrEmpty()) {
            binding.btnOpenGithub.visibility = View.VISIBLE
            binding.btnOpenGithub.setOnClickListener {
                FileViewerHelper.openUrl(requireContext(), project.githubRepo)
            }
        }

        binding.btnRateProject.setOnClickListener {
            showRateDialog(project)
        }

        initializePlayer(project)
    }

    private fun setupTabs() {
        binding.tabAbout.setOnClickListener {
            binding.scrollAbout.visibility = View.VISIBLE
            binding.rvReviews.visibility = View.GONE
            binding.tabAbout.setTextColor(requireContext().getColor(R.color.white))
            binding.tabAbout.setBackgroundResource(R.drawable.bg_tab_indicator)
            binding.tabReviews.setTextColor(requireContext().getColor(R.color.text_hint))
            binding.tabReviews.background = null
        }
        binding.tabReviews.setOnClickListener {
            binding.scrollAbout.visibility = View.GONE
            binding.rvReviews.visibility = View.VISIBLE
            binding.tabReviews.setTextColor(requireContext().getColor(R.color.white))
            binding.tabReviews.setBackgroundResource(R.drawable.bg_tab_indicator)
            binding.tabAbout.setTextColor(requireContext().getColor(R.color.text_hint))
            binding.tabAbout.background = null
            
            // Load reviews
            loadReviews()
        }
    }

    private fun loadReviews() {
        val ratings = mutableListOf<InstructorInteraction>()
        db.readableDatabase.rawQuery(
            "SELECT * FROM ratings WHERE entity_id = ? AND entity_type = 'project' ORDER BY timestamp DESC",
            arrayOf(projectId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id"))
                val user = db.getUserById(userId)
                ratings.add(InstructorInteraction(
                    type = "project_rating",
                    text = cursor.getString(cursor.getColumnIndexOrThrow("comment")) ?: "Rated ${cursor.getFloat(cursor.getColumnIndexOrThrow("rating_value"))} stars",
                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp")),
                    lessonTitle = "",
                    userName = user?.fullName ?: "Anonymous Student",
                    userId = userId,
                    rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating_value")),
                    interactionId = cursor.getLong(cursor.getColumnIndexOrThrow("rating_id"))
                ))
            }
        }
        
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        val interactionAdapter = com.unicoursehub.app.ui.adapters.InteractionAdapter()
        binding.rvReviews.adapter = interactionAdapter
        interactionAdapter.submitList(ratings)
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
                view.findViewById<RadioButton>(checkedId)?.text?.toString()
            } else null

            val session = SessionManager(context)
            db.rateProject(project.id, session.getUserId(), rating, badge, if (comment.isEmpty()) null else comment)
            
            Toast.makeText(context, R.string.msg_comment_success, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
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
        val activity = requireActivity() as com.unicoursehub.app.ui.main.MainActivity
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
        val activity = requireActivity() as? com.unicoursehub.app.ui.main.MainActivity
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        activity?.setToolbarVisibility(true)
        activity?.setBottomNavVisibility(true)
        
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
}
