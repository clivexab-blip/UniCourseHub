package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentDownloadsBinding
import com.unicoursehub.app.ui.adapters.DownloadsAdapter
import com.unicoursehub.app.ui.main.MainActivity

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val db = DatabaseHelper.getInstance(requireContext())
        val downloadedLessons = db.getDownloadedLessons()

        if (downloadedLessons.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvDownloads.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvDownloads.visibility = View.VISIBLE
            binding.rvDownloads.layoutManager = LinearLayoutManager(requireContext())
            binding.rvDownloads.adapter = DownloadsAdapter(downloadedLessons) { lesson ->
                // Navigate to VideoLearningFragment and pass the courseId and lessonId
                (activity as? MainActivity)?.showFragment(VideoLearningFragment.newInstance(lesson.courseId, lesson.id))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
