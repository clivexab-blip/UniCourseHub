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
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.databinding.FragmentCourseReviewBinding
import com.unicoursehub.app.ui.adapters.ModuleAdapter
import com.unicoursehub.app.ui.main.MainActivity

class CourseReviewFragment : Fragment() {

    private var _binding: FragmentCourseReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private var courseId: Long = -1
    private var player: ExoPlayer? = null
    private var isAdminReview: Boolean = false

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_IS_ADMIN = "is_admin"
        fun newInstance(courseId: Long, isAdmin: Boolean = false) = CourseReviewFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_COURSE_ID, courseId)
                putBoolean(ARG_IS_ADMIN, isAdmin)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCourseReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        courseId = arguments?.getLong(ARG_COURSE_ID) ?: -1
        isAdminReview = arguments?.getBoolean(ARG_IS_ADMIN) ?: false

        setupTabs()

        val course = db.getCourseById(courseId)
        binding.tvCourseTitle.text = course?.title ?: getString(R.string.title_course_review)
        binding.tvCourseDesc.text = course?.description ?: ""

        if (isAdminReview) {
            binding.adminActions.visibility = View.VISIBLE
            binding.btnCourseActions.visibility = View.VISIBLE
            binding.btnCourseActions.setOnClickListener { showAdminMenu(it) }
            
            binding.btnApprove.setOnClickListener { approveCourse() }
            binding.btnReject.setOnClickListener { rejectCourse() }
            binding.btnDeleteCourse.setOnClickListener { deleteCourse() }
        }

        val lessons = db.getLessonsForCourse(courseId)
        binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLessons.adapter = ModuleAdapter(
            lessons,
            onLessonClick = { lesson -> previewLesson(lesson) },
            onMenuClick = { lesson, view ->
                val popup = android.widget.PopupMenu(requireContext(), view)
                if (!lesson.linkedDocPath.isNullOrEmpty()) {
                    popup.menu.add("View Document")
                }
                popup.menu.add("View Questions")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "View Document" -> {
                            lesson.linkedDocPath?.let { com.unicoursehub.app.util.FileViewerHelper.viewDocument(requireContext(), it) }
                        }
                        "View Questions" -> {
                            Toast.makeText(requireContext(), getString(R.string.msg_lesson_comments, db.getCommentsForLesson(lesson.id).size), Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                popup.show()
            }
        )
    }

    private fun setupTabs() {
        binding.tabLectures.setOnClickListener {
            binding.scrollLectures.visibility = View.VISIBLE
            binding.scrollMore.visibility = View.GONE
            binding.tabLectures.setTextColor(requireContext().getColor(R.color.white))
            binding.tabLectures.setBackgroundResource(R.drawable.bg_tab_indicator)
            binding.tabMore.setTextColor(requireContext().getColor(R.color.text_hint))
            binding.tabMore.background = null
        }
        binding.tabMore.setOnClickListener {
            binding.scrollLectures.visibility = View.GONE
            binding.scrollMore.visibility = View.VISIBLE
            binding.tabMore.setTextColor(requireContext().getColor(R.color.white))
            binding.tabMore.setBackgroundResource(R.drawable.bg_tab_indicator)
            binding.tabLectures.setTextColor(requireContext().getColor(R.color.text_hint))
            binding.tabLectures.background = null
        }
    }

    private fun showAdminMenu(view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Approve Course")
        popup.menu.add("Reject Course")
        popup.menu.add("Delete Course")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Approve Course" -> approveCourse()
                "Reject Course" -> rejectCourse()
                "Delete Course" -> deleteCourse()
            }
            true
        }
        popup.show()
    }

    private fun approveCourse() {
        db.setCourseStatus(courseId, "published")
        Toast.makeText(requireContext(), R.string.msg_course_approved, Toast.LENGTH_SHORT).show()
        
        // Simulation: Notify Instructor via Cloud Push
        val course = db.getCourseById(courseId)
        course?.let {
            com.unicoursehub.app.util.PushNotificationSender.sendStatusNotification(
                requireContext(),
                it.instructorId,
                "Course Published! 🚀",
                "Congratulations! Your course '${it.title}' is now live for students."
            )
        }
        
        binding.adminActions.visibility = View.GONE
        binding.btnCourseActions.visibility = View.GONE
    }

    private fun rejectCourse() {
        db.setCourseStatus(courseId, "rejected")
        Toast.makeText(requireContext(), R.string.msg_course_rejected, Toast.LENGTH_SHORT).show()
        
        // Simulation: Notify Instructor via Cloud Push
        val course = db.getCourseById(courseId)
        course?.let {
            com.unicoursehub.app.util.PushNotificationSender.sendStatusNotification(
                requireContext(),
                it.instructorId,
                "Action Required ⚠️",
                "Your course '${it.title}' was not approved. Check instructor portal for details."
            )
        }
        
        binding.adminActions.visibility = View.GONE
        binding.btnCourseActions.visibility = View.GONE
    }

    private fun deleteCourse() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
        dialogView.findViewById<TextView>(R.id.tvMessage).text = getString(R.string.msg_confirm_delete_account)
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_close_circle)
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setColorFilter(requireContext().getColor(R.color.danger))
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
            text = getString(R.string.btn_delete)
            setOnClickListener {
                db.deleteCourse(courseId)
                Toast.makeText(requireContext(), R.string.msg_account_deleted, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun previewLesson(lesson: Lesson) {
        binding.tvCurrentLesson.text = lesson.title
        binding.videoPlaceholder.visibility = View.GONE
        if (lesson.type == "video") {
            binding.playerView.visibility = View.VISIBLE
            initializePlayer(lesson)
        } else {
            binding.playerView.visibility = View.GONE
            player?.release()
            player = null
            com.unicoursehub.app.util.FileViewerHelper.viewDocument(requireContext(), lesson.linkedDocPath ?: "")
        }
    }

    private fun initializePlayer(lesson: Lesson) {
        player?.release()
        player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            
            val videoUri = if (!lesson.localVideoPath.isNullOrEmpty()) {
                lesson.localVideoPath
            } else {
                lesson.videoUrl
            }

            if (videoUri != null) {
                // Ensure the URI is properly formatted for local files
                val uri = if (videoUri.startsWith("/")) "file://$videoUri" else videoUri
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                Toast.makeText(requireContext(), R.string.msg_no_video_source, Toast.LENGTH_SHORT).show()
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
        controller.findViewById<TextView>(R.id.btnPlaybackSpeed)?.setOnClickListener { view ->
            showSpeedMenu(view as TextView)
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

    private fun showSpeedMenu(anchor: TextView) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
        speeds.forEach { speed ->
            popup.menu.add("${speed}X")
        }
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
