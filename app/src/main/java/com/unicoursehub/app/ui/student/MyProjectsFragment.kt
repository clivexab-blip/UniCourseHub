package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentMyProjectsBinding
import com.unicoursehub.app.ui.adapters.ProjectStudentAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class MyProjectsFragment : Fragment() {

    private var _binding: FragmentMyProjectsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        binding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ProjectStudentAdapter(
            items = emptyList(),
            onClick = { project ->
                (requireActivity() as MainActivity).showFragment(ProjectDetailFragment.newInstance(project.id), addToBackStack = true)
            },
            onMenuClick = { project, view ->
                showProjectMenu(project, view)
            }
        )
        binding.rvProjects.adapter = adapter

        binding.btnNewProject.setOnClickListener {
            (requireActivity() as MainActivity).showFragment(PostProjectFragment(), addToBackStack = true)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val projects = db.getProjectsByStudent(session.getUserId())
        (binding.rvProjects.adapter as? ProjectStudentAdapter)?.updateData(projects)
        binding.tvEmptyState.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showProjectMenu(project: com.unicoursehub.app.data.Project, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add(getString(com.unicoursehub.app.R.string.btn_edit))
        popup.menu.add(getString(com.unicoursehub.app.R.string.btn_delete))

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                getString(com.unicoursehub.app.R.string.btn_edit) -> {
                    // Logic to edit project
                }
                getString(com.unicoursehub.app.R.string.btn_delete) -> {
                    showDeleteConfirm(project)
                }
            }
            true
        }
        popup.show()
    }

    private fun showDeleteConfirm(project: com.unicoursehub.app.data.Project) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(com.unicoursehub.app.R.layout.dialog_confirm_action, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), com.unicoursehub.app.R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.TextView>(com.unicoursehub.app.R.id.tvTitle).text = getString(com.unicoursehub.app.R.string.btn_delete)
        dialogView.findViewById<android.widget.TextView>(com.unicoursehub.app.R.id.tvMessage).text = "Are you sure you want to delete '${project.title}'?"
        dialogView.findViewById<android.widget.ImageView>(com.unicoursehub.app.R.id.ivIcon).apply {
            setImageResource(com.unicoursehub.app.R.drawable.ic_close_circle)
            setColorFilter(requireContext().getColor(com.unicoursehub.app.R.color.danger))
        }

        dialogView.findViewById<View>(com.unicoursehub.app.R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(com.unicoursehub.app.R.id.btnConfirm).apply {
            text = getString(com.unicoursehub.app.R.string.btn_delete)
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
