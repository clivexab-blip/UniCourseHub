package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentShowcaseBinding
import com.unicoursehub.app.ui.adapters.ShowcaseAdapter

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShowcaseFragment : Fragment() {

    private var _binding: FragmentShowcaseBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ShowcaseAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowcaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        binding.rvShowcase.layoutManager = LinearLayoutManager(requireContext())
        adapter = ShowcaseAdapter(emptyList())
        binding.rvShowcase.adapter = adapter

        lifecycleScope.launch {
            val projects = withContext(Dispatchers.IO) {
                db.getApprovedShowcaseProjects()
            }
            adapter.updateData(projects)
            binding.tvEmptyState.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
