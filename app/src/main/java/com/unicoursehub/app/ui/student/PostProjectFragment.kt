package com.unicoursehub.app.ui.student

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
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.data.workers.VideoCompressWorker
import com.unicoursehub.app.databinding.FragmentPostProjectBinding
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager
import java.io.File
import java.io.FileOutputStream

class PostProjectFragment : Fragment() {

    private var _binding: FragmentPostProjectBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private var courses = listOf<com.unicoursehub.app.data.Course>()
    private var isCompetition = false
    private var selectedVideoUri: Uri? = null
    private var selectedDocUri: Uri? = null

    companion object {
        private const val ARG_COMPETITION = "is_competition"
        fun newInstance(isCompetition: Boolean = false) = PostProjectFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_COMPETITION, isCompetition) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())
        isCompetition = arguments?.getBoolean(ARG_COMPETITION) ?: false

        // Fix: Only show courses the student is enrolled in
        val enrollments = db.getEnrollmentsForStudent(session.getUserId())
        courses = enrollments.map { it.second }
        
        val spinnerItems = mutableListOf<String>()
        spinnerItems.add(getString(com.unicoursehub.app.R.string.option_none_project))
        spinnerItems.addAll(courses.map { "${it.title} (${it.code})" })

        binding.spinnerCourse.adapter = ArrayAdapter(
            requireContext(), R.layout.item_spinner_white, spinnerItems
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }
        
        binding.spinnerType.adapter = ArrayAdapter(
            requireContext(), R.layout.item_spinner_white, listOf("Individual", "Group")
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }
        binding.spinnerSemester.adapter = ArrayAdapter(
            requireContext(), R.layout.item_spinner_white, listOf("Fall 2025", "Spring 2026", "Summer 2026")
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }

        binding.btnUploadVideo.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }

        binding.btnUploadDoc.setOnClickListener {
            docPickerLauncher.launch("application/*")
        }

        binding.btnPostProject.setOnClickListener { submit() }
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it
            binding.tvVideoLabel.text = getString(com.unicoursehub.app.R.string.label_video_selected, it.lastPathSegment)
            binding.tvVideoLabel.setTextColor(requireContext().getColor(com.unicoursehub.app.R.color.success))
        }
    }

    private val docPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedDocUri = it
            binding.tvDocLabel.text = getString(com.unicoursehub.app.R.string.label_doc_selected, it.lastPathSegment)
            binding.tvDocLabel.setTextColor(requireContext().getColor(com.unicoursehub.app.R.color.success))
        }
    }

    private fun submit() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val github = binding.etGithub.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            binding.tvError.text = getString(com.unicoursehub.app.R.string.error_fill_fields)
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val selectedIndex = binding.spinnerCourse.selectedItemPosition
        val selectedCourse = if (selectedIndex > 0) courses[selectedIndex - 1] else null
        
        val type = binding.spinnerType.selectedItem.toString()
        val semester = binding.spinnerSemester.selectedItem.toString()

        val localDocFile = selectedDocUri?.let { copyUriToTempFile(it, "project_doc") }

        val projectId = db.createProject(
            Project(
                title = title,
                studentId = session.getUserId(),
                relatedCourseId = selectedCourse?.id ?: 0,
                projectType = type,
                semester = semester,
                description = description,
                githubRepo = github,
                category = selectedCourse?.category ?: "General",
                isCompetitionEntry = isCompetition,
                linkedDocPath = localDocFile?.absolutePath
            )
        )

        // Trigger Video Compression if video selected
        selectedVideoUri?.let { uri ->
            val tempVideoFile = copyUriToTempFile(uri, "project_video")
            tempVideoFile?.let { file ->
                val inputData = Data.Builder()
                    .putString("input_path", file.absolutePath)
                    .putLong("project_id", projectId)
                    .putString("lesson_title", title)
                    .build()

                val compressRequest = OneTimeWorkRequestBuilder<VideoCompressWorker>()
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(requireContext()).enqueue(compressRequest)
            }
        }

        Toast.makeText(requireContext(), getString(R.string.msg_project_submitted_review), Toast.LENGTH_LONG).show()
        
        // Remove "Post Project" from backstack and replace with "Project Detail"
        parentFragmentManager.popBackStack()
        (requireActivity() as MainActivity).showFragment(ProjectDetailFragment.newInstance(projectId), addToBackStack = true)
    }

    private fun copyUriToTempFile(uri: Uri, prefix: String): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val extension = if (prefix.contains("video")) ".mp4" else ".pdf"
            val file = File(requireContext().filesDir, "${prefix}_${System.currentTimeMillis()}$extension")
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
