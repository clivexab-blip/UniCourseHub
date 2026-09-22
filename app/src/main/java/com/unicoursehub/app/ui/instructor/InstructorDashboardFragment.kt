package com.unicoursehub.app.ui.instructor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentInstructorDashboardBinding
import com.unicoursehub.app.ui.adapters.CourseOverviewAdapter
import com.unicoursehub.app.ui.adapters.ProjectReviewAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstructorDashboardFragment : Fragment() {

    private var _binding: FragmentInstructorDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var overviewAdapter: CourseOverviewAdapter
    private lateinit var reviewAdapter: ProjectReviewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInstructorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())

        // Authorization Fix: Only Instructor can see this content
        if (session.getRole() != "instructor") {
            binding.rvCourseOverview.visibility = View.GONE
            binding.rvRecentSubmissions.visibility = View.GONE
            android.widget.Toast.makeText(requireContext(), "Unauthorized access", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.rvCourseOverview.layoutManager = LinearLayoutManager(requireContext())
        overviewAdapter = CourseOverviewAdapter(emptyList()) { course ->
            (requireActivity() as MainActivity).showFragment(ContentManagementFragment.newInstance(course.id), addToBackStack = true)
        }
        binding.rvCourseOverview.adapter = overviewAdapter

        binding.rvRecentSubmissions.layoutManager = LinearLayoutManager(requireContext())
        reviewAdapter = ProjectReviewAdapter(
            emptyList(),
            onPreview = { (requireActivity() as MainActivity).showFragment(StudentProjectsFragment(), addToBackStack = true) }
        )
        binding.rvRecentSubmissions.adapter = reviewAdapter

        lifecycleScope.launch {
            val instructor = withContext(Dispatchers.IO) { db.getUserById(session.getUserId()) }
            val myCourses = withContext(Dispatchers.IO) { db.getCoursesByInstructor(session.getUserId()) }
            val studentCount = withContext(Dispatchers.IO) {
                myCourses.sumOf { db.countEnrolledStudentsForCourse(it.id) }
            }
            val pendingProjects = withContext(Dispatchers.IO) {
                db.getProjectsForInstructorCourses(session.getUserId()).filter { it.status == "pending" }
            }

            binding.tvWelcome.text = getString(
                R.string.welcome_back_format,
                instructor?.fullName ?: getString(R.string.instructor_default_name),
                "\uD83D\uDC69\u200D\uD83C\uDFEB"
            )
            binding.tvMyCourses.text = myCourses.size.toString()
            binding.tvStudentsCount.text = studentCount.toString()
            binding.tvPendingReviews.text = pendingProjects.size.toString()

            overviewAdapter.updateData(myCourses)
            reviewAdapter.updateData(pendingProjects.take(3))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
