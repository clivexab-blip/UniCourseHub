package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentStudentCoursesBinding
import com.unicoursehub.app.ui.adapters.CourseStudentAdapter
import com.unicoursehub.app.ui.adapters.StudentCourseRow
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentCoursesFragment : Fragment() {

    private var _binding: FragmentStudentCoursesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: CourseStudentAdapter
    private var currentFilter = "all"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        adapter = CourseStudentAdapter(
            onClick = { row ->
                if (row.statusKey == "not_enrolled") {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            db.enrollStudent(session.getUserId(), row.course.id)
                        }
                        com.unicoursehub.app.util.NotificationHelper(requireContext())
                            .showGeneralNotification(
                                getString(R.string.label_enrolled),
                                getString(R.string.msg_enroll_success, row.course.title)
                            )
                        Toast.makeText(requireContext(), getString(R.string.msg_enrolled_in_course, row.course.title), Toast.LENGTH_SHORT).show()
                        refresh("")
                    }
                } else {
                    (requireActivity() as MainActivity).showFragment(VideoLearningFragment.newInstance(row.course.id), addToBackStack = true)
                }
            },
            onInstructorClick = { instructorId ->
                (requireActivity() as MainActivity).showFragment(ViewInstructorFragment.newInstance(instructorId), addToBackStack = true)
            }
        )
        binding.rvCourses.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCourses.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.tabAll.setOnClickListener { selectTab("all") }
        binding.tabInProgress.setOnClickListener { selectTab("in_progress") }
        binding.tabCompleted.setOnClickListener { selectTab("completed") }
        binding.tabSaved.setOnClickListener { selectTab("saved") }
        
        refresh("")
    }

    override fun onResume() {
        super.onResume()
        refresh(binding.etSearch.text.toString())
    }

    private fun selectTab(filter: String) {
        currentFilter = filter
        val tabs = listOf(binding.tabAll to "all", binding.tabInProgress to "in_progress", binding.tabCompleted to "completed", binding.tabSaved to "saved")
        for ((tab, key) in tabs) {
            tab.setBackgroundResource(if (key == filter) R.drawable.bg_pill_purple else R.drawable.bg_pill_outline)
            tab.setTextColor(requireContext().getColor(if (key == filter) R.color.white else R.color.text_primary))
        }
        refresh(binding.etSearch.text.toString())
    }

    private fun refresh(query: String) {
        lifecycleScope.launch {
            val enrollments = withContext(Dispatchers.IO) { db.getEnrollmentsForStudent(session.getUserId()) }
            val enrolledCourseIds = enrollments.associate { it.second.id to it.first }
            val published = withContext(Dispatchers.IO) { db.getCoursesByStatus("published") }

            var rows = published.map { course ->
                val enrollment = enrolledCourseIds[course.id]
                if (enrollment != null) {
                    val labelRes = when (enrollment.status) {
                        "completed" -> R.string.status_completed
                        "saved" -> R.string.status_saved
                        else -> R.string.status_in_progress
                    }
                    StudentCourseRow(course, labelRes, enrollment.status)
                } else {
                    StudentCourseRow(course, R.string.btn_enroll, "not_enrolled")
                }
            }

            if (currentFilter != "all") rows = rows.filter { it.statusKey == currentFilter }
            if (query.isNotBlank()) rows = rows.filter { it.course.title.contains(query, ignoreCase = true) }
            adapter.submitList(rows)
            binding.tvEmptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
