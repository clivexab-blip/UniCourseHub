package com.unicoursehub.app.ui.instructor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentInstructorInteractionsBinding
import com.unicoursehub.app.ui.adapters.InteractionAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.ui.student.ChatFragment
import com.unicoursehub.app.util.SessionManager

class InstructorInteractionsFragment : Fragment() {
    private var _binding: FragmentInstructorInteractionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInstructorInteractionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val instructorId = session.getUserId()

        binding.rvInteractions.layoutManager = LinearLayoutManager(requireContext())
        val adapter = InteractionAdapter(
            onReply = { interaction ->
                (requireActivity() as MainActivity).showFragment(
                    ChatFragment.newInstance(interaction.userId, interaction.userName)
                )
            },
            onDelete = { interaction ->
                when (interaction.type) {
                    "comment" -> db.deleteComment(interaction.interactionId)
                    "confusing" -> db.deleteConfusingReport(interaction.interactionId)
                    "lesson_rating", "instructor_rating" -> db.deleteRating(interaction.interactionId)
                }
                refreshList(instructorId)
            }
        )
        binding.rvInteractions.adapter = adapter
        refreshList(instructorId)
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(requireContext())
        refreshList(session.getUserId())
    }

    private fun refreshList(instructorId: Long) {
        val db = DatabaseHelper.getInstance(requireContext())
        val items = db.getAllInteractionsForInstructor(instructorId)
        (binding.rvInteractions.adapter as? InteractionAdapter)?.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
