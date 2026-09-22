package com.unicoursehub.app.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentNotificationsBinding
import com.unicoursehub.app.ui.adapters.NotificationAdapter
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        val adapter = NotificationAdapter()
        binding.rvNotifications.adapter = adapter

        val notifications = db.getNotificationsForUser(session.getUserId())
        adapter.submitList(notifications)
        
        binding.tvEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE

        // Mark all as read when opening the screen
        binding.rvNotifications.post {
            db.markNotificationsRead(session.getUserId())
            (requireActivity() as? MainActivity)?.refreshPendingBadge()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
