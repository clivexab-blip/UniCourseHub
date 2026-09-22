package com.unicoursehub.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentAdminDashboardBinding
import com.unicoursehub.app.ui.main.MainActivity
import com.unicoursehub.app.util.SessionManager

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val admin = db.getUserById(session.getUserId())

        binding.tvWelcome.text = getString(
            R.string.welcome_back_format,
            admin?.fullName ?: getString(R.string.admin_default_name),
            "🛡️"
        )
        binding.tvTotalUsers.text = db.countAllUsers().toString()
        binding.tvTotalCourses.text = db.countAllCourses().toString()
        binding.tvPendingApprovals.text = db.countPendingApprovals().toString()

        val activity = requireActivity() as MainActivity
        binding.cardManageUsers.setOnClickListener { activity.showFragment(ManageUsersFragment(), addToBackStack = true) }
        binding.cardManageCourses.setOnClickListener { activity.showFragment(ManageCoursesFragment(), addToBackStack = true) }
        binding.cardManageProjects.setOnClickListener { activity.showFragment(ManageProjectsFragment(), addToBackStack = true) }
        binding.cardPendingApprovals.setOnClickListener { activity.showFragment(PendingApprovalsFragment(), addToBackStack = true) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
