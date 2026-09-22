package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentReferencesBinding
import com.unicoursehub.app.ui.adapters.ReferenceAdapter
import com.unicoursehub.app.util.SessionManager
import com.unicoursehub.app.util.AiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.lifecycle.lifecycleScope

class ReferencesFragment : Fragment() {

    private var _binding: FragmentReferencesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: ReferenceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())

        setupRecyclerView()
        loadReferences()
    }

    private fun setupRecyclerView() {
        adapter = ReferenceAdapter(
            emptyList(),
            onPlayClick = { lesson ->
                // Navigate to VideoLearningFragment with this lesson's course
                val fragment = VideoLearningFragment.newInstance(lesson.courseId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onSaveNotes = { lesson, notes ->
                lifecycleScope.launch {
                    val summary = if (notes.length > 10) {
                        withContext(Dispatchers.IO) { 
                            val result = AiHelper.callGemini("Summarize these study notes in one short PLAIN TEXT sentence (no Markdown): $notes")
                            if (result is AiHelper.AiResult.Success) result.text else null
                        }
                    } else null
                    
                    db.updateReferenceNotes(session.getUserId(), lesson.id, notes, summary)
                    Toast.makeText(requireContext(), getString(R.string.msg_notes_saved), Toast.LENGTH_SHORT).show()
                    loadReferences()
                }
            },
            onDeleteClick = { lesson ->
                db.deleteReference(session.getUserId(), lesson.id)
                loadReferences()
            }
        )
        binding.rvReferences.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReferences.adapter = adapter
    }

    private fun loadReferences() {
        val references = db.getReferencesForUser(session.getUserId())
        if (references.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvReferences.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvReferences.visibility = View.VISIBLE
            adapter.updateData(references)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
