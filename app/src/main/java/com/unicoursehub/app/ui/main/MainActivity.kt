package com.unicoursehub.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.ActivityMainBinding
import com.unicoursehub.app.ui.admin.*
import com.unicoursehub.app.ui.auth.LoginActivity
import com.unicoursehub.app.ui.instructor.*
import com.unicoursehub.app.ui.student.*
import com.unicoursehub.app.util.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper.getInstance(this)
        session = SessionManager(this)

        if (!session.isLoggedIn()) {
            goToLogin()
            return
        }

        // Fix: ALWAYS synchronize SessionManager role with the SQLite database user record
        val currentUserId = session.getUserId()
        if (currentUserId != -1L) {
            val dbUser = db.getUserById(currentUserId)
            if (dbUser != null && dbUser.role.isNotEmpty()) {
                session.saveSession(dbUser.id, dbUser.role)
            }
        }

        setupToolbarTitle()
        setupBottomNav()
        setupMenuButtons()
        requestNotificationPermission()
        subscribeToAllTopics()
        captureFcmToken()
        refreshPendingBadge()
    }

    private fun captureFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM", "Captured Device Token: $token")
                db.updateFcmToken(session.getUserId(), token)
            }
        }
    }

    private fun subscribeToAllTopics() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM", "Subscribed to 'all' topic successfully")
                }
            }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
    }

    private fun setupToolbarTitle() {
        val title = when (session.getRole()) {
            "admin" -> getString(R.string.title_admin_panel)
            "instructor" -> getString(R.string.title_instructor_portal)
            else -> getString(R.string.title_student_dashboard)
        }
        binding.tvToolbarTitle.text = title
    }

    private fun setupBottomNav() {
        when (session.getRole()) {
            "admin" -> {
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_admin)
                binding.bottomNav.setOnItemSelectedListener {
                    when (it.itemId) {
                        R.id.nav_dashboard -> showFragment(AdminDashboardFragment(), "admin_dash", addToBackStack = false, clearBackStack = true)
                        R.id.nav_users -> showFragment(ManageUsersFragment(), "manage_users", addToBackStack = false, clearBackStack = true)
                        R.id.nav_courses -> showFragment(ManageCoursesFragment(), "manage_courses", addToBackStack = false, clearBackStack = true)
                        R.id.nav_pending -> showFragment(PendingApprovalsFragment(), "pending", addToBackStack = false, clearBackStack = true)
                    }
                    true
                }
                showFragment(AdminDashboardFragment(), "admin_dash", addToBackStack = false, clearBackStack = true)
            }
            "instructor" -> {
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_instructor)
                binding.bottomNav.setOnItemSelectedListener {
                    when (it.itemId) {
                        R.id.nav_home -> showFragment(InstructorDashboardFragment(), "inst_dash", addToBackStack = false, clearBackStack = true)
                        R.id.nav_courses -> showFragment(MyCoursesFragment(), "my_courses", addToBackStack = false, clearBackStack = true)
                        R.id.nav_students -> showFragment(MyStudentsFragment(), "my_students", addToBackStack = false, clearBackStack = true)
                        R.id.nav_projects -> showFragment(StudentProjectsFragment(), "student_projects", addToBackStack = false, clearBackStack = true)
                    }
                    true
                }
                showFragment(InstructorDashboardFragment(), "inst_dash", addToBackStack = false, clearBackStack = true)
            }
            else -> {
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_student)
                binding.bottomNav.setOnItemSelectedListener {
                    when (it.itemId) {
                        R.id.nav_home -> showFragment(StudentDashboardFragment(), "stud_dash", addToBackStack = false, clearBackStack = true)
                        R.id.nav_courses -> showFragment(StudentCoursesFragment(), "stud_courses", addToBackStack = false, clearBackStack = true)
                        R.id.nav_learn -> showFragment(VideoLearningFragment(), "video_learn", addToBackStack = false, clearBackStack = true)
                        R.id.nav_projects -> showFragment(MyProjectsFragment(), "my_projects", addToBackStack = false, clearBackStack = true)
                    }
                    true
                }
                showFragment(StudentDashboardFragment(), "stud_dash", addToBackStack = false, clearBackStack = true)
            }
        }
    }

    private fun setupMenuButtons() {
        binding.btnBell.setOnClickListener {
            showFragment(NotificationsFragment(), "notifications", addToBackStack = true)
        }

        binding.btnMenu.setOnClickListener { anchor ->
            val menuRes = when (session.getRole()) {
                "admin" -> R.menu.menu_overflow_admin
                "instructor" -> R.menu.menu_overflow_instructor
                else -> R.menu.menu_overflow_student
            }
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(menuRes, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_profile -> {
                        showFragment(
                            when (session.getRole()) {
                                "admin" -> AdminProfileFragment()
                                "instructor" -> InstructorProfileFragment()
                                else -> StudentProfileFragment()
                            },
                            addToBackStack = true
                        )
                        true
                    }
                    R.id.menu_showcase -> { showFragment(ShowcaseFragment(), addToBackStack = true); true }
                    R.id.menu_insights -> { showFragment(InsightsFragment(), addToBackStack = true); true }
                    R.id.menu_competition -> { showFragment(CompetitionFragment(), addToBackStack = true); true }
                    R.id.menu_messages -> { showFragment(MessagesFragment(), addToBackStack = true); true }
                    R.id.menu_interactions -> { showFragment(InstructorInteractionsFragment(), addToBackStack = true); true }
                    R.id.menu_downloads -> { showFragment(DownloadsFragment(), addToBackStack = true); true }
                    R.id.menu_references -> { showFragment(ReferencesFragment(), addToBackStack = true); true }
                    R.id.menu_settings -> { showFragment(SettingsFragment(), addToBackStack = true); true }
                    R.id.menu_post_project -> { showFragment(PostProjectFragment(), addToBackStack = true); true }
                    R.id.menu_post_course -> { showFragment(PostCourseFragment(), addToBackStack = true); true }
                    R.id.menu_logout -> { logout(); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    /** Public helper so fragments can navigate to another fragment (e.g. dashboard cards, edit/delete, sub-screens). */
    fun showFragment(
        fragment: Fragment,
        tag: String? = null,
        addToBackStack: Boolean = true,
        clearBackStack: Boolean = false
    ) {
        if (clearBackStack) {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment, tag)

        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }

        transaction.commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    fun refreshPendingBadge() {
        val count = if (session.getRole() == "admin") {
            db.countPendingApprovals()
        } else {
            db.countUnreadNotifications(session.getUserId())
        }
        binding.tvBellBadge.visibility = if (count > 0) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvBellBadge.text = count.toString()
    }

    private fun logout() {
        session.logout()
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        com.unicoursehub.app.util.GoogleAuthHelper.client(this).signOut()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun setToolbarVisibility(visible: Boolean) {
        binding.btnMenu.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvToolbarTitle.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnBell.parent.let { (it as? android.view.View)?.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE }
        (binding.btnMenu.parent as? android.view.View)?.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    fun setBottomNavVisibility(visible: Boolean) {
        binding.bottomNav.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        binding.navDivider.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }
}
