package com.unicoursehub.app.ui.instructor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentPostCourseBinding
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class PostCourseFragment : Fragment() {

    private var _binding: FragmentPostCourseBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private val categories = listOf("Web Dev", "Mobile Dev", "Databases", "AI/ML", "Other")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        binding.spinnerCategory.adapter = ArrayAdapter(
            requireContext(), R.layout.item_spinner_white, categories
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }
        binding.spinnerSemester.adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_white,
            listOf("Fall 2025", "Spring 2026", "Summer 2026")
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }

        binding.btnCreateCourse.setOnClickListener { submit() }
    }

    private fun submit() {
        val title = binding.etTitle.text.toString().trim()
        val code = binding.etCode.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty() || code.isEmpty() || description.isEmpty()) {
            binding.tvError.text = getString(R.string.error_fill_fields)
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val category = categories[binding.spinnerCategory.selectedItemPosition]
        val semester = binding.spinnerSemester.selectedItem.toString()
        val tileColor = when (category) {
            "Mobile Dev" -> "pink"
            "Databases" -> "blue"
            "AI/ML" -> "orange"
            else -> "purple"
        }

        val courseId = db.createCourse(
            Course(
                title = title,
                code = code,
                category = category,
                semester = semester,
                description = description,
                instructorId = session.getUserId(),
                status = "draft",
                tileColor = tileColor
            )
        )

        Toast.makeText(requireContext(), getString(R.string.msg_course_created_add_lessons), Toast.LENGTH_LONG).show()
        
        // Remove "Post Course" from backstack and replace with "Content Management"
        parentFragmentManager.popBackStack()
        (requireActivity() as MainActivity).showFragment(ContentManagementFragment.newInstance(courseId), addToBackStack = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
