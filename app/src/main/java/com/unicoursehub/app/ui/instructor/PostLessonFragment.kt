package com.unicoursehub.app.ui.instructor

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.data.workers.VideoCompressWorker
import com.unicoursehub.app.databinding.FragmentPostLessonBinding
import java.io.File
import java.io.FileOutputStream

class PostLessonFragment : Fragment() {

    private var _binding: FragmentPostLessonBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private var courseId: Long = -1
    private var lessonId: Long = -1
    private var selectedFileUri: Uri? = null
    private var selectedDocUri: Uri? = null
    private val types = listOf("video", "doc")

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_LESSON_ID = "lesson_id"
        fun newInstance(courseId: Long, lessonId: Long = -1) = PostLessonFragment().apply {
            arguments = Bundle().apply { 
                putLong(ARG_COURSE_ID, courseId)
                putLong(ARG_LESSON_ID, lessonId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        courseId = arguments?.getLong(ARG_COURSE_ID) ?: -1
        lessonId = arguments?.getLong(ARG_LESSON_ID) ?: -1

        binding.spinnerType.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_white, types).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }

        if (lessonId != -1L) {
            loadLessonData()
        }

        binding.btnUploadContent.setOnClickListener {
            val type = types[binding.spinnerType.selectedItemPosition]
            if (type == "video") {
                videoPickerLauncher.launch("video/*")
            } else {
                docPickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            }
        }

        binding.btnUploadDoc.setOnClickListener {
            optionalDocPickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }

        binding.btnAddLesson.setOnClickListener { submit() }
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleFileSelection(it) }
    }

    private val docPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleFileSelection(it) }
    }

    private val optionalDocPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            selectedDocUri = it
            binding.tvDocFileName.text = getString(R.string.label_doc_selected, it.lastPathSegment ?: "File")
        }
    }

    private fun handleFileSelection(uri: Uri) {
        selectedFileUri = uri
        binding.tvFileName.text = getString(R.string.label_file_selected, uri.lastPathSegment)
        binding.tvFileName.visibility = View.VISIBLE
    }

    private fun loadLessonData() {
        val lesson = db.getLessonsForCourse(courseId).find { it.id == lessonId } ?: return
        binding.etModule.setText(lesson.moduleName)
        binding.etTitle.setText(lesson.title)
        binding.etDuration.setText(lesson.duration)
        binding.spinnerType.setSelection(types.indexOf(lesson.type))
        binding.cbHasQuiz.isChecked = lesson.hasQuiz
        
        if (!lesson.localVideoPath.isNullOrEmpty()) {
            binding.tvFileName.text = "Existing: ${File(lesson.localVideoPath).name}"
            binding.tvFileName.visibility = View.VISIBLE
        }
        
        binding.btnAddLesson.text = "Save Changes"
    }

    private fun submit() {
        val module = binding.etModule.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val duration = binding.etDuration.text.toString().trim()
        val type = types[binding.spinnerType.selectedItemPosition]

        if (module.isEmpty() || title.isEmpty() || (lessonId == -1L && selectedFileUri == null)) {
            binding.tvError.text = getString(R.string.error_fill_fields_upload)
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val existingLesson = if (lessonId != -1L) db.getLessonsForCourse(courseId).find { it.id == lessonId } else null
        
        val tempFile = selectedFileUri?.let { copyUriToTempFile(it, if (type == "video") "mp4" else null) }
        val tempDocFile = selectedDocUri?.let { copyUriToTempFile(it, null) }
        
        val newLesson = Lesson(
            id = lessonId.coerceAtLeast(0),
            courseId = courseId,
            moduleName = module,
            title = title,
            type = type,
            status = if (tempFile != null && type == "video") "processing" else (existingLesson?.status ?: "published"),
            duration = if (type == "video") duration else "-",
            orderIndex = existingLesson?.orderIndex ?: (db.getLessonsForCourse(courseId).size + 1),
            localVideoPath = if (type == "video") (tempFile?.absolutePath ?: existingLesson?.localVideoPath) else null,
            videoUrl = if (type == "doc") (tempFile?.absolutePath ?: existingLesson?.videoUrl) else null,
            linkedDocPath = tempDocFile?.absolutePath ?: existingLesson?.linkedDocPath,
            hasQuiz = binding.cbHasQuiz.isChecked
        )

        if (lessonId == -1L) {
            val id = db.addLesson(newLesson)
            if (type == "video" && tempFile != null) startCompression(id, tempFile.absolutePath, title)
        } else {
            // Reset rating only if a new video file was uploaded
            val resetRating = (type == "video" && selectedFileUri != null)
            db.updateLesson(newLesson, resetRating)
            if (type == "video" && tempFile != null) startCompression(lessonId, tempFile.absolutePath, title)
        }

        Toast.makeText(requireContext(), R.string.msg_lesson_added, Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun startCompression(id: Long, path: String, title: String) {
        val data = Data.Builder()
            .putString("input_path", path)
            .putLong("lesson_id", id)
            .putString("lesson_title", title)
            .build()

        val compressRequest = OneTimeWorkRequestBuilder<VideoCompressWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(compressRequest)
    }

    private fun copyUriToTempFile(uri: Uri, forcedExtension: String?): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            
            // Try to determine the real file extension
            var extension = forcedExtension
            if (extension == null) {
                val mimeType = contentResolver.getType(uri)
                extension = when (mimeType) {
                    "application/pdf" -> "pdf"
                    "application/msword" -> "doc"
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                    else -> "bin"
                }
            }

            val file = File(requireContext().filesDir, "lesson_content_${System.currentTimeMillis()}.$extension")
            FileOutputStream(file).use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
