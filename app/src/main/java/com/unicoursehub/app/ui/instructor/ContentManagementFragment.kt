package com.unicoursehub.app.ui.instructor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.data.Lesson
import com.unicoursehub.app.databinding.FragmentContentManagementBinding
import com.unicoursehub.app.ui.adapters.ModuleAdapter
import com.unicoursehub.app.ui.admin.CourseReviewFragment
import com.unicoursehub.app.ui.main.MainActivity

class ContentManagementFragment : Fragment() {

    private var _binding: FragmentContentManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private var courseId: Long = -1

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        fun newInstance(courseId: Long) = ContentManagementFragment().apply {
            arguments = Bundle().apply { putLong(ARG_COURSE_ID, courseId) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())
        courseId = arguments?.getLong(ARG_COURSE_ID) ?: -1

        val course = db.getCourseById(courseId)
        binding.tvCourseTitle.text = course?.title ?: getString(R.string.title_content_management)

        binding.btnAddLesson.setOnClickListener {
            (requireActivity() as? MainActivity)?.showFragment(
                PostLessonFragment.newInstance(courseId)
            )
        }

        binding.tvCourseTitle.setOnClickListener {
            (requireActivity() as? MainActivity)?.showFragment(
                CourseReviewFragment.newInstance(courseId, isAdmin = false)
            )
        }

        binding.btnSubmitFullCourse.setOnClickListener { submitCourse() }
        
        binding.btnDeleteCourse.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
            val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
                .setView(dialogView)
                .create()
            
            dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.settings_delete)
            dialogView.findViewById<android.widget.TextView>(R.id.tvMessage).text = "Are you sure you want to delete this entire course? This will remove all lessons and enrollments."
            dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_close_circle)
            dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setColorFilter(requireContext().getColor(R.color.danger))
            
            dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
            dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
                text = getString(R.string.btn_delete)
                setOnClickListener {
                    db.deleteCourse(courseId)
                    (requireActivity() as MainActivity).showFragment(MyCoursesFragment())
                    dialog.dismiss()
                }
            }
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
        
        refresh()
    }

    private fun submitCourse() {
        db.setCourseStatus(courseId, "pending")
        android.widget.Toast.makeText(requireContext(), getString(R.string.msg_course_submitted_approval), android.widget.Toast.LENGTH_LONG).show()
        (requireActivity() as? MainActivity)?.showFragment(MyCoursesFragment())
    }

    private fun refresh() {
        val lessons = db.getLessonsForCourse(courseId)
        val course = db.getCourseById(courseId)

        // Show submit button only if in draft and has lessons
        if (course?.status == "draft" && lessons.isNotEmpty()) {
            binding.btnSubmitFullCourse.visibility = View.VISIBLE
        } else {
            binding.btnSubmitFullCourse.visibility = View.GONE
        }

        binding.rvModules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvModules.adapter = ModuleAdapter(
            lessons,
            onLessonClick = { lesson ->
                if (lesson.type == "video") {
                    (requireActivity() as MainActivity).showFragment(
                        com.unicoursehub.app.ui.admin.CourseReviewFragment.newInstance(courseId, isAdmin = false),
                        addToBackStack = true
                    )
                }
            },
            onMenuClick = { lesson, view ->
                showLessonActions(lesson, view)
            }
        )
        binding.tvNoLessons.visibility = if (lessons.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showLessonActions(lesson: Lesson, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Edit Lesson")
        popup.menu.add("Delete Lesson")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit Lesson" -> {
                    (requireActivity() as MainActivity).showFragment(
                        PostLessonFragment.newInstance(courseId, lesson.id)
                    )
                }
                "Delete Lesson" -> {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
                    val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
                        .setView(dialogView)
                        .create()
                    
                    dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = "Delete Lesson"
                    dialogView.findViewById<android.widget.TextView>(R.id.tvMessage).text = "Are you sure you want to delete this lesson? This will remove all ratings and comments for this video."
                    dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_close_circle)
                    dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon).setColorFilter(requireContext().getColor(R.color.danger))
                    
                    dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
                    dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
                        text = getString(R.string.btn_delete)
                        setOnClickListener {
                            db.deleteLesson(lesson.id)
                            refresh()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
