package com.unicoursehub.app.ui.instructor

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
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.FragmentStudentProjectsBinding
import com.unicoursehub.app.ui.adapters.ProjectReviewAdapter
import com.unicoursehub.app.util.FileViewerHelper
import com.unicoursehub.app.util.SessionManager

class StudentProjectsFragment : Fragment() {

    private var _binding: FragmentStudentProjectsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: ProjectReviewAdapter
    private var currentFilter = "all"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        adapter = ProjectReviewAdapter(
            items = emptyList(),
            onPreview = { project -> 
                (requireActivity() as com.unicoursehub.app.ui.main.MainActivity).showFragment(
                    com.unicoursehub.app.ui.admin.ProjectReviewFragment.newInstance(project.id),
                    addToBackStack = true
                )
            },
            onMenuClick = { project, view ->
                val popup = android.widget.PopupMenu(requireContext(), view)
                if (!project.localVideoPath.isNullOrEmpty() || !project.videoUrl.isNullOrEmpty()) {
                    popup.menu.add("Watch Demo")
                }
                if (!project.linkedDocPath.isNullOrEmpty()) {
                    popup.menu.add("View Document")
                }
                popup.menu.add("Rate & Review")
                
                popup.setOnMenuItemClickListener { item ->
                    val intentToReview = {
                        (requireActivity() as com.unicoursehub.app.ui.main.MainActivity).showFragment(
                            com.unicoursehub.app.ui.admin.ProjectReviewFragment.newInstance(project.id),
                            addToBackStack = true
                        )
                    }
                    when (item.title) {
                        "Watch Demo", "Rate & Review" -> intentToReview()
                        "View Document" -> com.unicoursehub.app.util.FileViewerHelper.viewDocument(requireContext(), project.linkedDocPath!!)
                    }
                    true
                }
                popup.show()
            }
        )
        binding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProjects.adapter = adapter

        binding.tabAll.setOnClickListener { selectTab("all") }
        binding.tabPending.setOnClickListener { selectTab("pending") }
        binding.tabApproved.setOnClickListener { selectTab("approved") }
    }

    override fun onStart() {
        super.onStart()
        selectTab(currentFilter)
    }

    private fun selectTab(filter: String) {
        currentFilter = filter
        val tabs = listOf(binding.tabAll to "all", binding.tabPending to "pending", binding.tabApproved to "approved")
        for ((tab, key) in tabs) {
            tab.setBackgroundResource(if (key == filter) R.drawable.bg_pill_purple else R.drawable.bg_pill_outline)
            tab.setTextColor(requireContext().getColor(if (key == filter) R.color.white else R.color.text_primary))
        }
        val all = db.getProjectsForInstructorCourses(session.getUserId())
        adapter.updateData(if (filter == "all") all else all.filter { it.status == filter })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
