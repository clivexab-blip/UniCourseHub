package com.unicoursehub.app.ui.instructor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentMyCoursesBinding
import com.unicoursehub.app.ui.adapters.CourseInstructorAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class MyCoursesFragment : Fragment() {

    private var _binding: FragmentMyCoursesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        binding.rvCourses.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCourses.adapter = CourseInstructorAdapter(
            emptyList(),
            onClick = { course ->
                (requireActivity() as MainActivity).showFragment(ContentManagementFragment.newInstance(course.id), addToBackStack = true)
            },
            onMenuClick = { course, view ->
                val popup = android.widget.PopupMenu(requireContext(), view)
                popup.menu.add("Manage Content")
                popup.menu.add("Delete Course")
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Manage Content" -> (requireActivity() as MainActivity).showFragment(ContentManagementFragment.newInstance(course.id), addToBackStack = true)
                        "Delete Course" -> showDeleteConfirm(course)
                    }
                    true
                }
                popup.show()
            }
        )

        binding.btnCreateCourse.setOnClickListener {
            (requireActivity() as MainActivity).showFragment(PostCourseFragment(), addToBackStack = true)
        }
    }

    private fun showDeleteConfirm(course: com.unicoursehub.app.data.Course) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
        dialogView.findViewById<android.widget.TextView>(R.id.tvMessage).text = "Are you sure you want to delete '${course.title}'?"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).apply {
            setImageResource(R.drawable.ic_close_circle)
            setColorFilter(requireContext().getColor(R.color.danger))
        }
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
            text = getString(R.string.btn_delete)
            setOnClickListener {
                db.deleteCourse(course.id)
                refresh()
                dialog.dismiss()
            }
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
        val courses = db.getCoursesByInstructor(session.getUserId())
        (binding.rvCourses.adapter as? CourseInstructorAdapter)?.updateData(courses)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
