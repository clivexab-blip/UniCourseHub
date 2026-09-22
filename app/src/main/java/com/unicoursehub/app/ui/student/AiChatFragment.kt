package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.ChatMessage
import com.unicoursehub.app.databinding.FragmentAiChatBinding
import com.unicoursehub.app.ui.adapters.ChatAdapter
import com.unicoursehub.app.util.AiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AiChatFragment : Fragment() {

    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatAdapter
    private var lessonTitle: String = ""
    
    private var previousInteractionId: String? = null
    private val messagesList = mutableListOf<ChatMessage>()

    companion object {
        private const val ARG_LESSON_ID = "lesson_id"
        private const val ARG_LESSON_TITLE = "lesson_title"
        fun newInstance(lessonId: Long, lessonTitle: String) = AiChatFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_LESSON_ID, lessonId)
                putString(ARG_LESSON_TITLE, lessonTitle)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lessonTitle = arguments?.getString(ARG_LESSON_TITLE) ?: ""

        binding.tvLessonContext.text = "Topic: $lessonTitle"

        // Student ID: 0L, AI ID: 1L
        adapter = ChatAdapter(0L)
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = adapter

        if (messagesList.isEmpty()) {
            addMessage(1L, "Hello! I'm your SeniorDev AI assistant. I've been briefed on '$lessonTitle'. Ask me anything about this topic!")
        } else {
            adapter.submitList(messagesList.toList())
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessageToAi(text)
                binding.etMessage.setText("")
            }
        }
    }

    private fun addMessage(senderId: Long, text: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val msg = ChatMessage(messagesList.size.toLong(), senderId, if(senderId == 0L) 1L else 0L, text, time)
        messagesList.add(msg)
        adapter.submitList(messagesList.toList())
        binding.rvChat.post {
            binding.rvChat.smoothScrollToPosition(messagesList.size - 1)
        }
    }

    private fun sendMessageToAi(userText: String) {
        addMessage(0L, userText)
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false
        
        lifecycleScope.launch {
            // Context injection for first message - requesting plain text with no Markdown headers or bullets
            val finalPrompt = if (previousInteractionId == null) {
                "You are a Senior Android Developer mentor. Provide concise, expert advice in plain text. " +
                "DO NOT use Markdown (no #, *, **, or / markers). Topic: '$lessonTitle'. Student: $userText"
            } else userText

            val result = withContext(Dispatchers.IO) {
                AiHelper.callGemini(finalPrompt, previousInteractionId)
            }
            
            binding.progressBar.visibility = View.GONE
            binding.btnSend.isEnabled = true
            
            when (result) {
                is AiHelper.AiResult.Success -> {
                    previousInteractionId = result.interactionId
                    addMessage(1L, result.text)
                }
                is AiHelper.AiResult.Error -> {
                    addMessage(1L, "⚠️ ${result.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
