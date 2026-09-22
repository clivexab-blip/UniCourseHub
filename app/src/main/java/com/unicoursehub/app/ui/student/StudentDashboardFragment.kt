package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentStudentDashboardBinding
import com.unicoursehub.app.ui.adapters.ContinueLearningAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentDashboardFragment : Fragment() {

    private var _binding: FragmentStudentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ContinueLearningAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        
        binding.rvContinueLearning.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContinueLearningAdapter(
            onClick = { course ->
                (requireActivity() as MainActivity).showFragment(VideoLearningFragment.newInstance(course.id), addToBackStack = true)
            },
            onInstructorClick = { instructorId ->
                (requireActivity() as MainActivity).showFragment(ViewInstructorFragment.newInstance(instructorId), addToBackStack = true)
            }
        )
        binding.rvContinueLearning.adapter = adapter

        lifecycleScope.launch {
            val student = withContext(Dispatchers.IO) { db.getUserById(session.getUserId()) }
            val enrollments = withContext(Dispatchers.IO) { db.getEnrollmentsForStudent(session.getUserId()) }
            val projects = withContext(Dispatchers.IO) { db.getProjectsByStudent(session.getUserId()) }

            binding.tvWelcome.text = getString(
                R.string.welcome_back_format,
                student?.fullName ?: getString(R.string.student_default_name),
                "\uD83D\uDC4B"
            )
            binding.tvEnrolledCourses.text = enrollments.size.toString()
            binding.tvMyProjects.text = projects.size.toString()
            binding.tvHoursLearned.text = (enrollments.size * 8).toString()

            val inProgress = enrollments.filter { it.first.status == "in_progress" }
            adapter.submitList(inProgress)
            binding.tvEmptyState.visibility = if (enrollments.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
