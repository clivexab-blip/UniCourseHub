package com.unicoursehub.app.ui.instructor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.User
import com.unicoursehub.app.databinding.FragmentMyStudentsBinding
import com.unicoursehub.app.ui.adapters.StudentRowAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class MyStudentsFragment : Fragment() {

    private var _binding: FragmentMyStudentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var rows: List<Pair<User, Pair<String, Int>>>
    private lateinit var adapter: StudentRowAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val myCourses = db.getCoursesByInstructor(session.getUserId())

        rows = myCourses.flatMap { course ->
            db.getAllUsers().filter { it.role == "student" }.mapNotNull { student ->
                val enrollment = db.getEnrollmentsForStudent(student.id).find { it.second.id == course.id }
                enrollment?.let { student to (course.title to it.first.progress) }
            }
        }

        adapter = StudentRowAdapter(rows) { student ->
            (requireActivity() as MainActivity).showFragment(
                com.unicoursehub.app.ui.student.ChatFragment.newInstance(student.id, student.fullName)
            )
        }
        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = adapter
        binding.tvEmptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                adapter.updateData(if (query.isBlank()) rows else rows.filter { it.first.fullName.contains(query, true) })
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun selectTab(filter: String) {
        // Method not used in current structure but kept for potential future filtering
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
