package com.unicoursehub.app.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.Project
import com.unicoursehub.app.databinding.FragmentManageProjectsBinding
import com.unicoursehub.app.ui.adapters.ProjectReviewAdapter
import com.unicoursehub.app.util.FileViewerHelper

class ManageProjectsFragment : Fragment() {

    private var _binding: FragmentManageProjectsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: ProjectReviewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())

        adapter = ProjectReviewAdapter(
            items = emptyList(),
            onPreview = { project ->
                (requireActivity() as com.unicoursehub.app.ui.main.MainActivity).showFragment(
                    ProjectReviewFragment.newInstance(project.id),
                    addToBackStack = true
                )
            }
        )
        binding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProjects.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val projects = db.getAllProjects()
        adapter.updateData(projects)
        binding.tvEmpty.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
