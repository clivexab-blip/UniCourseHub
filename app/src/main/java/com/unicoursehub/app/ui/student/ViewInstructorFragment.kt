package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentViewInstructorBinding
import com.unicoursehub.app.ui.adapters.CourseStudentAdapter
import com.unicoursehub.app.ui.adapters.StudentCourseRow
import com.unicoursehub.app.ui.adapters.InteractionAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager
import com.unicoursehub.app.ui.dialogs.MessageDialog
import android.widget.Toast
import android.app.AlertDialog

class ViewInstructorFragment : Fragment() {
    private var _binding: FragmentViewInstructorBinding? = null
    private val binding get() = _binding!!
    private var instructorId: Long = -1
    private lateinit var session: SessionManager

    companion object {
        private const val ARG_INSTRUCTOR_ID = "instructor_id"
        fun newInstance(instructorId: Long) = ViewInstructorFragment().apply {
            arguments = Bundle().apply { putLong(ARG_INSTRUCTOR_ID, instructorId) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentViewInstructorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        instructorId = arguments?.getLong(ARG_INSTRUCTOR_ID) ?: -1
        val db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        val instructor = db.getUserById(instructorId)
        val courses = db.getCoursesByInstructor(instructorId)
        val avgRating = db.getInstructorAverageRating(instructorId)

        binding.tvInstructorName.text = instructor?.fullName ?: "Instructor"
        binding.tvUniversity.text = instructor?.university ?: ""
        
        // Apply half-star logic for modern UI
        val roundedRating = if (avgRating % 1 >= 0.5f) {
            avgRating.toInt() + 0.5f
        } else {
            avgRating.toInt().toFloat()
        }
        binding.instructorRating.rating = roundedRating

        // Show message button only if enrolled
        if (session.getRole() == "student" && db.isStudentEnrolledWithInstructor(session.getUserId(), instructorId)) {
            binding.btnMessageInstructor.visibility = View.VISIBLE
            binding.btnMessageInstructor.setOnClickListener {
                MessageDialog.show(requireContext(), session.getUserId(), instructorId, instructor?.fullName ?: "Instructor")
            }
        }

        binding.rvPublishedCourses.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CourseStudentAdapter(
            onClick = { row ->
                (requireActivity() as MainActivity).showFragment(VideoLearningFragment.newInstance(row.course.id), addToBackStack = true)
            },
            onInstructorClick = { /* Already viewing this instructor */ }
        )
        binding.rvPublishedCourses.adapter = adapter
        
        adapter.submitList(courses.map { StudentCourseRow(it, com.unicoursehub.app.R.string.status_published, "published") })

        // Load Student Reviews
        binding.rvStudentReviews.layoutManager = LinearLayoutManager(requireContext())
        val reviewAdapter = InteractionAdapter()
        binding.rvStudentReviews.adapter = reviewAdapter
        
        val reviews = db.getAllInteractionsForInstructor(instructorId).filter { it.rating > 0 }
        reviewAdapter.submitList(reviews)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
