package com.unicoursehub.app.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.Course
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentManageCoursesBinding
import com.unicoursehub.app.ui.adapters.CourseAdminAdapter
import com.unicoursehub.app.util.SessionManager

class ManageCoursesFragment : Fragment() {

    private var _binding: FragmentManageCoursesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: CourseAdminAdapter
    private var currentFilter = "all"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        val session = SessionManager(requireContext())

        // Authorization Fix: Only Admin can see this content
        if (session.getRole() != "admin") {
            binding.rvCourses.visibility = View.GONE
            binding.tabAll.visibility = View.GONE
            binding.tabPublished.visibility = View.GONE
            binding.tabPending.visibility = View.GONE
            android.widget.Toast.makeText(requireContext(), "Unauthorized access", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        adapter = CourseAdminAdapter(
            db.getAllCourses(),
            actionLabel = { if (it.status == "pending") R.string.btn_review else R.string.btn_edit },
            onAction = { course ->
                (requireActivity() as com.unicoursehub.app.ui.main.MainActivity).showFragment(
                    CourseReviewFragment.newInstance(course.id, isAdmin = true),
                    addToBackStack = true
                )
            },
            onMenuClick = { course, view ->
                showCourseMenu(course, view)
            }
        )
        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = adapter

        binding.tabAll.setOnClickListener { selectTab("all") }
        binding.tabPublished.setOnClickListener { selectTab("published") }
        binding.tabPending.setOnClickListener { selectTab("pending") }
    }

    private fun showCourseMenu(course: Course, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        
        if (course.status == "pending") {
            popup.menu.add("Approve Course")
            popup.menu.add("Reject Course")
        }
        popup.menu.add("Delete Course")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Approve Course" -> {
                    db.setCourseStatus(course.id, "published")
                    android.widget.Toast.makeText(requireContext(), R.string.msg_course_approved, android.widget.Toast.LENGTH_SHORT).show()
                    selectTab(currentFilter)
                }
                "Reject Course" -> {
                    db.setCourseStatus(course.id, "rejected")
                    android.widget.Toast.makeText(requireContext(), R.string.msg_course_rejected, android.widget.Toast.LENGTH_SHORT).show()
                    selectTab(currentFilter)
                }
                "Delete Course" -> {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
                    val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
                        .setView(dialogView)
                        .create()
                    
                    dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
                    dialogView.findViewById<android.widget.TextView>(R.id.tvMessage).text = "Are you sure you want to delete '${course.title}'?"
                    dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).apply {
                        setImageResource(R.drawable.ic_close_circle)
                        setColorFilter(requireContext().getColor(R.color.danger))
                    }
                    
                    dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
                    dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
                        text = getString(R.string.btn_delete)
                        setOnClickListener {
                            db.deleteCourse(course.id)
                            selectTab(currentFilter)
                            dialog.dismiss()
                        }
                    }
                    
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.show()
                }
            }
            true
        }
        popup.show()
    }

    private fun selectTab(filter: String) {
        currentFilter = filter
        binding.tabAll.setBackgroundResource(if (filter == "all") R.drawable.bg_pill_purple else R.drawable.bg_pill_outline)
        binding.tabAll.setTextColor(requireContext().getColor(if (filter == "all") R.color.white else R.color.text_primary))
        binding.tabPublished.setBackgroundResource(if (filter == "published") R.drawable.bg_pill_purple else R.drawable.bg_pill_outline)
        binding.tabPublished.setTextColor(requireContext().getColor(if (filter == "published") R.color.white else R.color.text_primary))
        binding.tabPending.setBackgroundResource(if (filter == "pending") R.drawable.bg_pill_purple else R.drawable.bg_pill_outline)
        binding.tabPending.setTextColor(requireContext().getColor(if (filter == "pending") R.color.white else R.color.text_primary))

        val all = db.getAllCourses()
        adapter.updateData(if (filter == "all") all else all.filter { it.status == filter })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
