package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentMessagesListBinding
import com.unicoursehub.app.ui.adapters.ConversationAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class MessagesFragment : Fragment() {
    private var _binding: FragmentMessagesListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ConversationAdapter { user ->
            (requireActivity() as MainActivity).showFragment(
                ChatFragment.newInstance(user.id, user.fullName)
            )
        }
        binding.rvMessages.adapter = adapter

        val conversations = db.getChatConversations(session.getUserId())
        adapter.submitList(conversations)
        
        binding.tvEmpty.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
