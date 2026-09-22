package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentCompetitionBinding
import com.unicoursehub.app.ui.main.MainActivity

class CompetitionFragment : Fragment() {

    private var _binding: FragmentCompetitionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCompetitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val entries = db.getCompetitionEntries()

        binding.leaderboardContainer.removeAllViews()
        val medals = listOf("\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49")
        entries.take(5).forEachIndexed { index, project ->
            val row = TextView(requireContext())
            val medal = medals.getOrElse(index) { "${index + 1}." }
            row.text = getString(R.string.leaderboard_row_format, medal, project.studentName, project.title)
            row.setTextColor(requireContext().getColor(com.unicoursehub.app.R.color.text_primary))
            row.textSize = 14f
            row.setPadding(0, 12, 0, 12)
            binding.leaderboardContainer.addView(row)
        }
        binding.tvNoEntries.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        binding.btnSubmitEntry.setOnClickListener {
            (requireActivity() as MainActivity).showFragment(PostProjectFragment.newInstance(isCompetition = true))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
