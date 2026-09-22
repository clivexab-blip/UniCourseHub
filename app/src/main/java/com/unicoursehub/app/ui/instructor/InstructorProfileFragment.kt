package com.unicoursehub.app.ui.instructor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentProfileBinding
import com.unicoursehub.app.ui.auth.LoginActivity
import com.unicoursehub.app.util.CourseVisuals
import com.unicoursehub.app.util.SessionManager

class InstructorProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val user = db.getUserById(session.getUserId()) ?: return
        val myCourses = db.getCoursesByInstructor(user.id)
        val myProjects = db.getProjectsForInstructorCourses(user.id)

        with(binding) {
            tvAvatar.text = CourseVisuals.initials(user.fullName)
            tvName.text = user.fullName
            tvRole.text = getString(com.unicoursehub.app.R.string.role_instructor)
            tvStat1Value.text = myCourses.size.toString()
            tvStat1Label.text = getString(com.unicoursehub.app.R.string.stat_courses)
            tvStat2Value.text = myProjects.size.toString()
            tvStat2Label.text = getString(com.unicoursehub.app.R.string.stat_projects)
            tvStat3Value.text = (myCourses.size * 12).toString()
            tvStat3Label.text = getString(com.unicoursehub.app.R.string.stat_hours)
            tvEmail.text = user.email
            tvStudentId.text = user.studentId.ifEmpty { "—" }
            tvRoleRow.text = getString(com.unicoursehub.app.R.string.role_instructor)
            tvMemberSince.text = getString(com.unicoursehub.app.R.string.member_since_format, user.createdAt.ifEmpty { "2024" })

            btnLogout.setOnClickListener {
                session.logout()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
