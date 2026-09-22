package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentChatBinding
import com.unicoursehub.app.ui.adapters.ChatAdapter
import com.unicoursehub.app.util.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: ChatAdapter
    private var otherUserId: Long = -1L
    private var otherUserName: String = ""

    companion object {
        fun newInstance(otherUserId: Long, otherUserName: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong("other_user_id", otherUserId)
                putString("other_user_name", otherUserName)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        otherUserId = arguments?.getLong("other_user_id") ?: -1L
        otherUserName = arguments?.getString("other_user_name") ?: "Chat"

        // For demo: if no other user, default to Sphumelele (Instructor)
        if (otherUserId == -1L) otherUserId = 3L

        adapter = ChatAdapter(session.getUserId())
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter

        refreshMessages()
        startPolling()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                db.sendMessage(session.getUserId(), otherUserId, text)
                
                // Simulation: Notify Other User via Cloud Push
                val currentUser = db.getUserById(session.getUserId())
                com.unicoursehub.app.util.PushNotificationSender.sendMessageNotification(
                    requireContext(),
                    otherUserId,
                    currentUser?.fullName ?: "Someone",
                    text
                )

                binding.etMessage.setText("")
                refreshMessages()
                binding.rvChat.postDelayed({
                    binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)
                }, 100)
            }
        }
    }

    private fun refreshMessages() {
        val messages = db.getMessagesBetween(session.getUserId(), otherUserId)
        adapter.submitList(messages)
    }

    private fun startPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                refreshMessages()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
