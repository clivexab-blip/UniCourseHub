package com.unicoursehub.app.ui.student

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.data.workers.DownloadWorker
import com.unicoursehub.app.databinding.FragmentVideoLearningBinding
import com.unicoursehub.app.ui.adapters.ModuleAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoLearningFragment : Fragment() {

    private var _binding: FragmentVideoLearningBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null
    private var currentLesson: Lesson? = null

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_LESSON_ID = "lesson_id"
        
        fun newInstance(courseId: Long, lessonId: Long = -1L) = VideoLearningFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_COURSE_ID, courseId)
                putLong(ARG_LESSON_ID, lessonId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val contextThemeWrapper = android.view.ContextThemeWrapper(requireContext(), R.style.Theme_UniCourseHub_Dark)
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        _binding = FragmentVideoLearningBinding.inflate(localInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())

        var courseId = arguments?.getLong(ARG_COURSE_ID) ?: -1L
        val targetLessonId = arguments?.getLong(ARG_LESSON_ID) ?: -1L

        setupTabs()

        lifecycleScope.launch {
            if (courseId == -1L) {
                val enrollments = withContext(Dispatchers.IO) { db.getEnrollmentsForStudent(session.getUserId()) }
                val firstInProgress = enrollments.firstOrNull { it.first.status == "in_progress" }
                courseId = firstInProgress?.second?.id ?: -1L
            }

            if (courseId == -1L) {
                binding.tvNoCourse.visibility = View.VISIBLE
                return@launch
            }

            val course = withContext(Dispatchers.IO) { db.getCourseById(courseId) }
            val lessons = withContext(Dispatchers.IO) { db.getLessonsForCourse(courseId) }
            val enrollmentData = withContext(Dispatchers.IO) { db.getEnrollmentsForStudent(session.getUserId()) }
            val enrollment = enrollmentData.find { it.second.id == courseId }?.first

            binding.tvCourseTitle.text = course?.title ?: getString(R.string.label_course)
            binding.tvCompletion.text = getString(R.string.video_completion_format, enrollment?.progress ?: 0)
            binding.tvRating.text = String.format("%.1f", course?.rating ?: 0.0f)
            binding.tvCourseDesc.text = course?.description ?: ""

            binding.btnMessageInstructor.setOnClickListener {
                course?.let { c ->
                    (requireActivity() as MainActivity).showFragment(
                        ChatFragment.newInstance(c.instructorId, c.instructorName.ifEmpty { getString(R.string.label_instructor) }),
                        addToBackStack = true
                    )
                }
            }

            val initialLesson = if (targetLessonId != -1L) {
                lessons.find { it.id == targetLessonId } ?: lessons.firstOrNull()
            } else {
                lessons.firstOrNull()
            }

            if (initialLesson != null) {
                selectLesson(initialLesson)
            } else {
                binding.tvCurrentLesson.text = getString(R.string.msg_no_lessons_available)
            }

            binding.rvModules.layoutManager = LinearLayoutManager(requireContext())
            binding.rvModules.adapter = ModuleAdapter(
                lessons,
                onLessonClick = { lesson ->
                    selectLesson(lesson)
                    lifecycleScope.launch {
                        val milestoneReached = withContext(Dispatchers.IO) {
                            db.markLessonWatched(lesson.id)
                            db.updateCourseProgress(session.getUserId(), courseId)
                        }
                        
                        if (milestoneReached) {
                            showMilestoneDialog()
                        }

                        val newEnrollments = withContext(Dispatchers.IO) { db.getEnrollmentsForStudent(session.getUserId()) }
                        val newEnrollment = newEnrollments.find { it.second.id == courseId }?.first
                        binding.tvCompletion.text = getString(R.string.video_completion_format, newEnrollment?.progress ?: 0)
                    }
                },
                onMenuClick = { lesson, view ->
                    showLessonMenu(lesson, view)
                },
                onDownloadClick = { lesson ->
                    startDownload(lesson)
                }
            )
        }
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

    private fun showLessonMenu(lesson: Lesson, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        val session = SessionManager(requireContext())
        val db = DatabaseHelper.getInstance(requireContext())

        // Minimalist Text-Only Menu Items
        popup.menu.add("Confusing Content")
        if (lesson.localVideoPath.isNullOrEmpty()) {
            popup.menu.add("Download Video")
        }
        popup.menu.add("Save Notes")
        if (!lesson.linkedDocPath.isNullOrEmpty()) {
            popup.menu.add("View Document")
        }
        if (lesson.hasQuiz) {
            popup.menu.add("Take Quiz")
        }
        popup.menu.add("Post a Question")
        popup.menu.add("Chat with AI")
        popup.menu.add("Message Instructor")
        popup.menu.add("Rate this Video")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Confusing Content" -> {
                    db.reportConfusingContent(lesson.id, session.getUserId())
                    Toast.makeText(requireContext(), R.string.msg_feedback_sent_instructor, Toast.LENGTH_SHORT).show()
                }
                "Download Video" -> {
                    startDownload(lesson)
                }
                "Save Notes" -> {
                    showSaveNotesDialog(lesson)
                }
                "View Document" -> {
                    lesson.linkedDocPath?.let { com.unicoursehub.app.util.FileViewerHelper.viewDocument(requireContext(), it) }
                }
                "Take Quiz" -> {
                    Toast.makeText(requireContext(), "Quiz feature coming soon!", Toast.LENGTH_SHORT).show()
                }
                "Post a Question" -> {
                    currentLesson = lesson
                    showCommentDialog()
                }
                "Chat with AI" -> {
                    (requireActivity() as MainActivity).showFragment(
                        AiChatFragment.newInstance(lesson.id, lesson.title),
                        addToBackStack = true
                    )
                }
                "Message Instructor" -> {
                    db.getCourseById(lesson.courseId)?.let { c ->
                        (requireActivity() as MainActivity).showFragment(
                            ChatFragment.newInstance(c.instructorId, c.instructorName.ifEmpty { getString(R.string.label_instructor) }),
                            addToBackStack = true
                        )
                    }
                }
                "Rate this Video" -> {
                    currentLesson = lesson
                    showRatingDialog()
                }
            }
            true
        }
        popup.show()
    }

    private fun showSaveNotesDialog(lesson: Lesson) {
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val userId = session.getUserId()
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_notes, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()
        
        // Load existing notes
        val existingNotes = db.getReferenceNotes(userId, lesson.id) ?: ""
        
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvLessonTitle)
        val tvModule = dialogView.findViewById<TextView>(R.id.tvModuleName)
        val btnSave = dialogView.findViewById<View>(R.id.btnSave)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)
        
        tvTitle.text = lesson.title
        tvModule.text = lesson.moduleName
        etNotes.setText(existingNotes)
        
        btnSave.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            lifecycleScope.launch {
                val summary = if (notes.length > 10) {
                    withContext(Dispatchers.IO) { 
                        val result = com.unicoursehub.app.util.AiHelper.callGemini("Summarize these study notes in one short PLAIN TEXT sentence (no Markdown): $notes")
                        if (result is com.unicoursehub.app.util.AiHelper.AiResult.Success) result.text else null
                    }
                } else null
                
                db.addReference(userId, lesson.id) // Ensure record exists
                db.updateReferenceNotes(userId, lesson.id, notes, summary)
                Toast.makeText(requireContext(), R.string.msg_notes_saved, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showRatingDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_simple_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val btnSubmit = dialogView.findViewById<View>(R.id.btnSubmit)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        btnSubmit.setOnClickListener {
            setRating(null, ratingBar.rating.toInt())
            dialog.dismiss()
        }
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showCommentDialog() {
        val lesson = currentLesson ?: return
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())

        player?.pause()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ask_question, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        val etInput = dialogView.findViewById<EditText>(R.id.etInput)
        val btnPost = dialogView.findViewById<View>(R.id.btnPost)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        btnPost.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val user = db.getUserById(session.getUserId())
                db.addComment(lesson.id, session.getUserId(), user?.fullName ?: getString(R.string.student_default_name), text)
                dialog.dismiss()
                showSuccessPopup(getString(R.string.msg_comment_success))
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showMilestoneDialog() {
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etComment = dialogView.findViewById<EditText>(R.id.etComment)
        val btnSubmit = dialogView.findViewById<View>(R.id.btnSubmit)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        player?.pause()

        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnSubmit.setOnClickListener {
            val ratingValue = ratingBar.rating
            val commentText = etComment.text.toString().trim()
            
            currentLesson?.let { lesson ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val course = db.getCourseById(lesson.courseId)
                    
                    // Rate the Instructor directly with a comment
                    if (course != null) {
                        db.rateInstructor(course.instructorId, session.getUserId(), ratingValue, commentText)
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), R.string.msg_comment_success, Toast.LENGTH_LONG).show()
                    }
                }
            }
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showSuccessPopup(message: String) {
        player?.pause()
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_success, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvMessage).text = message
        dialogView.findViewById<View>(R.id.btnOk).setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun selectLesson(lesson: Lesson) {
        currentLesson = lesson
        binding.tvCurrentLesson.text = lesson.title
        binding.videoPlaceholder.visibility = View.GONE
        
        // Show decimal average
        binding.tvRating.text = String.format(java.util.Locale.getDefault(), "%.1f", lesson.rating)

        initializePlayer(lesson)
    }

    private fun initializePlayer(lesson: Lesson) {
        player?.release()
        player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            val videoUri = lesson.localVideoPath ?: lesson.videoUrl
            if (videoUri != null) {
                val uri = if (videoUri.startsWith("/")) "file://$videoUri" else videoUri
                exoPlayer.setMediaItem(MediaItem.fromUri(uri))
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
        controller.findViewById<View>(R.id.exo_settings)?.setOnClickListener { view ->
            currentLesson?.let { showLessonMenu(it, view) }
        }
        controller.findViewById<TextView>(R.id.btnPlaybackSpeed)?.setOnClickListener { view ->
            showSpeedMenu(view as TextView)
        }
    }

    private fun toggleFullscreen() {
        val activity = requireActivity() as MainActivity
        val decorView = activity.window.decorView
        
        if (activity.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            // Switch to Portrait
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            binding.playerContainer.layoutParams.height = (220 * resources.displayMetrics.density).toInt()
            
            // Show system bars
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            
            // Show Activity UI
            activity.setToolbarVisibility(true)
            activity.setBottomNavVisibility(true)
        } else {
            // Switch to Landscape
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            binding.playerContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            
            // Hide system bars (Immersive mode)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            
            // Hide Activity UI
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

    private fun startDownload(lesson: Lesson? = null) {
        val target = lesson ?: currentLesson ?: return
        if (!target.localVideoPath.isNullOrEmpty()) return
        val videoUrl = target.videoUrl ?: return

        val downloadData = Data.Builder().putString("video_url", videoUrl).putLong("lesson_id", target.id).build()
        WorkManager.getInstance(requireContext()).enqueue(OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(downloadData).build())
        Toast.makeText(requireContext(), R.string.msg_download_started, Toast.LENGTH_SHORT).show()
    }

    private fun setRating(stars: List<ImageView>?, rating: Int) {
        val session = SessionManager(requireContext())
        val db = DatabaseHelper.getInstance(requireContext())
        
        stars?.forEachIndexed { index, star ->
            star.setColorFilter(requireContext().getColor(if (index < rating) R.color.warning else R.color.text_hint))
        }
        
        currentLesson?.let { lesson ->
            lifecycleScope.launch(Dispatchers.IO) {
                db.rateLesson(lesson.id, session.getUserId(), rating.toFloat())
                // Re-fetch lessons to get updated average
                val updatedLessons = db.getLessonsForCourse(lesson.courseId)
                val updatedLesson = updatedLessons.find { it.id == lesson.id }
                
                withContext(Dispatchers.Main) {
                    updatedLesson?.let {
                        currentLesson = it
                        binding.tvRating.text = String.format(java.util.Locale.getDefault(), "%.1f", it.rating)
                    }
                    Toast.makeText(requireContext(), getString(R.string.msg_video_rated, rating), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop() { super.onStop(); player?.pause() }
    override fun onDestroyView() {
        // Reset orientation and UI visibility when leaving
        val activity = requireActivity() as? MainActivity
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
