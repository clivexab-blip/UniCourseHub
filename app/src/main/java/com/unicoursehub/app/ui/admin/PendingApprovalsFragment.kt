package com.unicoursehub.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentPendingApprovalsBinding
import com.unicoursehub.app.ui.adapters.PendingCourseAdapter
import com.unicoursehub.app.ui.adapters.PendingProjectAdapter
import com.unicoursehub.app.ui.main.MainActivity

class PendingApprovalsFragment : Fragment() {

    private var _binding: FragmentPendingApprovalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPendingApprovalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
        val pendingCourses = db.getCoursesByStatus("pending")
        val pendingProjects = db.getProjectsByStatus("pending")

        binding.rvPendingCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingCourses.adapter = PendingCourseAdapter(
            pendingCourses,
            onApprove = { approveCourse(it) },
            onReject = { rejectCourse(it) },
            onItemClick = {
                (requireActivity() as? MainActivity)?.showFragment(
                    CourseReviewFragment.newInstance(it.id, isAdmin = true),
                    addToBackStack = true
                )
            },
            onMenuClick = { course, view -> showCourseMenu(course, view) }
        )
        binding.tvNoPendingCourses.visibility = if (pendingCourses.isEmpty()) View.VISIBLE else View.GONE

        binding.rvPendingProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingProjects.adapter = PendingProjectAdapter(
            pendingProjects,
            onItemClick = {
                (requireActivity() as? MainActivity)?.showFragment(
                    ProjectReviewFragment.newInstance(it.id),
                    addToBackStack = true
                )
            },
            onMenuClick = { project, view -> showProjectMenu(project, view) }
        )
        binding.tvNoPendingProjects.visibility = if (pendingProjects.isEmpty()) View.VISIBLE else View.GONE

        (activity as? MainActivity)?.refreshPendingBadge()
    }

    private fun showCourseMenu(course: com.unicoursehub.app.data.Course, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Approve Course")
        popup.menu.add("Reject Course")
        popup.menu.add("Delete Course")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Approve Course" -> approveCourse(course)
                "Reject Course" -> rejectCourse(course)
                "Delete Course" -> deleteCourse(course)
            }
            true
        }
        popup.show()
    }

    private fun approveCourse(course: com.unicoursehub.app.data.Course) {
        db.setCourseStatus(course.id, "published")
        android.widget.Toast.makeText(requireContext(), R.string.msg_course_approved, android.widget.Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun rejectCourse(course: com.unicoursehub.app.data.Course) {
        db.setCourseStatus(course.id, "rejected")
        android.widget.Toast.makeText(requireContext(), R.string.msg_course_rejected, android.widget.Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun deleteCourse(course: com.unicoursehub.app.data.Course) {
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

    private fun showProjectMenu(project: com.unicoursehub.app.data.Project, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Approve Project")
        popup.menu.add("Reject Project")
        popup.menu.add("Delete Project")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Approve Project" -> approveProject(project)
                "Reject Project" -> rejectProject(project)
                "Delete Project" -> deleteProject(project)
            }
            true
        }
        popup.show()
    }

    private fun approveProject(project: com.unicoursehub.app.data.Project) {
        db.setProjectStatus(project.id, "approved")
        Toast.makeText(requireContext(), R.string.msg_project_approved, Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun rejectProject(project: com.unicoursehub.app.data.Project) {
        db.setProjectStatus(project.id, "rejected")
        Toast.makeText(requireContext(), R.string.msg_project_rejected, Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun deleteProject(project: com.unicoursehub.app.data.Project) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
        dialogView.findViewById<android.widget.TextView>(R.id.tvMessage).text = "Are you sure you want to delete '${project.title}'?"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).apply {
            setImageResource(R.drawable.ic_close_circle)
            setColorFilter(requireContext().getColor(R.color.danger))
        }
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
            text = getString(R.string.btn_delete)
            setOnClickListener {
                db.deleteProject(project.id)
                refresh()
                dialog.dismiss()
            }
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
