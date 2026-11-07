package com.unicoursehub.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.unicoursehub.app.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Central SQLite database helper for UniCourse Hub.
 *
 * Tables (matches the ERD in the project brief):
 *  - users        (user_id PK, firebase_uid, full_name, email, university, student_id, role, status, created_at)
 *  - courses      (course_id PK, title, code, category, semester, description, instructor_id FK, status,
 *                  students_count, completion_rate, tile_color)
 *  - lessons      (lesson_id PK, course_id FK, module_name, title, type, status, duration, order_index, watched)
 *  - enrollments  (enrollment_id PK, student_id FK, course_id FK, progress, status)
 *  - projects     (project_id PK, title, student_id FK, related_course_id FK, project_type, semester,
 *                  description, github_repo, category, status, submitted_date, is_competition_entry)
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val DATABASE_NAME = "unicourse_hub.db"
        private const val DATABASE_VERSION = 15

        // Table names
        const val TABLE_USERS = "users"
        const val TABLE_COURSES = "courses"
        const val TABLE_LESSONS = "lessons"
        const val TABLE_ENROLLMENTS = "enrollments"
        const val TABLE_PROJECTS = "projects"
        const val TABLE_REFERENCES = "user_references"
        const val TABLE_COMMENTS = "comments"
        const val TABLE_CONFUSING_REPORTS = "confusing_reports"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_NOTIFICATIONS = "notifications"
        const val TABLE_RATINGS = "ratings"
        const val TABLE_QUIZ_QUESTIONS = "quiz_questions"

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context).also { INSTANCE = it }
            }
        }

        fun now(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                firebase_uid TEXT UNIQUE,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                university TEXT,
                student_id TEXT,
                role TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'active',
                created_at TEXT,
                rating REAL DEFAULT 0.0,
                rating_count INTEGER DEFAULT 0,
                fcm_token TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_COURSES (
                course_id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                code TEXT,
                category TEXT,
                semester TEXT,
                description TEXT,
                instructor_id INTEGER,
                status TEXT NOT NULL DEFAULT 'pending',
                students_count INTEGER DEFAULT 0,
                completion_rate INTEGER DEFAULT 0,
                tile_color TEXT DEFAULT 'purple',
                rating REAL DEFAULT 0.0,
                rating_count INTEGER DEFAULT 0,
                FOREIGN KEY(instructor_id) REFERENCES $TABLE_USERS(user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_LESSONS (
                lesson_id INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id INTEGER NOT NULL,
                module_name TEXT,
                title TEXT NOT NULL,
                type TEXT DEFAULT 'video',
                status TEXT DEFAULT 'draft',
                duration TEXT,
                order_index INTEGER DEFAULT 0,
                watched INTEGER DEFAULT 0,
                video_url TEXT,
                local_video_path TEXT,
                linked_doc_path TEXT,
                has_quiz INTEGER DEFAULT 0,
                rating REAL DEFAULT 0.0,
                rating_count INTEGER DEFAULT 0,
                FOREIGN KEY(course_id) REFERENCES $TABLE_COURSES(course_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_REFERENCES (
                ref_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                lesson_id INTEGER NOT NULL,
                notes TEXT,
                ai_summary TEXT,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id),
                FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_ENROLLMENTS (
                enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                course_id INTEGER NOT NULL,
                progress INTEGER DEFAULT 0,
                status TEXT DEFAULT 'in_progress',
                FOREIGN KEY(student_id) REFERENCES $TABLE_USERS(user_id),
                FOREIGN KEY(course_id) REFERENCES $TABLE_COURSES(course_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PROJECTS (
                project_id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                student_id INTEGER NOT NULL,
                related_course_id INTEGER,
                project_type TEXT DEFAULT 'Individual',
                semester TEXT,
                description TEXT,
                github_repo TEXT,
                category TEXT,
                status TEXT DEFAULT 'pending',
                submitted_date TEXT,
                is_competition_entry INTEGER DEFAULT 0,
                video_url TEXT,
                local_video_path TEXT,
                linked_doc_path TEXT,
                rating REAL DEFAULT 0.0,
                rating_count INTEGER DEFAULT 0,
                badge TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_USERS(user_id),
                FOREIGN KEY(related_course_id) REFERENCES $TABLE_COURSES(course_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_COMMENTS (
                comment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                lesson_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                user_name TEXT,
                text TEXT NOT NULL,
                timestamp TEXT,
                FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id),
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CONFUSING_REPORTS (
                report_id INTEGER PRIMARY KEY AUTOINCREMENT,
                lesson_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                timestamp TEXT,
                FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id),
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                message_id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_id INTEGER NOT NULL,
                receiver_id INTEGER NOT NULL,
                text TEXT NOT NULL,
                timestamp TEXT,
                is_read INTEGER DEFAULT 0,
                FOREIGN KEY(sender_id) REFERENCES $TABLE_USERS(user_id),
                FOREIGN KEY(receiver_id) REFERENCES $TABLE_USERS(user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp TEXT,
                is_read INTEGER DEFAULT 0,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_RATINGS (
                rating_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                entity_id INTEGER NOT NULL,
                entity_type TEXT NOT NULL,
                rating_value REAL NOT NULL,
                comment TEXT,
                timestamp TEXT,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_QUIZ_QUESTIONS (
                question_id INTEGER PRIMARY KEY AUTOINCREMENT,
                lesson_id INTEGER NOT NULL,
                question_text TEXT NOT NULL,
                option_a TEXT NOT NULL,
                option_b TEXT NOT NULL,
                option_c TEXT NOT NULL,
                option_d TEXT NOT NULL,
                correct_option INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id)
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_firebase ON $TABLE_USERS(firebase_uid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_email ON $TABLE_USERS(email)")

        seedData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // PERMANENT FIX: Ensure all tables exist if skipping onCreate during a multi-version jump
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_USERS (user_id INTEGER PRIMARY KEY AUTOINCREMENT, firebase_uid TEXT UNIQUE, full_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, university TEXT, student_id TEXT, role TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'active', created_at TEXT, rating REAL DEFAULT 0.0, rating_count INTEGER DEFAULT 0, fcm_token TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_COURSES (course_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, code TEXT, category TEXT, semester TEXT, description TEXT, instructor_id INTEGER, status TEXT NOT NULL DEFAULT 'pending', students_count INTEGER DEFAULT 0, completion_rate INTEGER DEFAULT 0, tile_color TEXT DEFAULT 'purple', rating REAL DEFAULT 0.0, rating_count INTEGER DEFAULT 0, FOREIGN KEY(instructor_id) REFERENCES $TABLE_USERS(user_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_LESSONS (lesson_id INTEGER PRIMARY KEY AUTOINCREMENT, course_id INTEGER NOT NULL, module_name TEXT, title TEXT NOT NULL, type TEXT DEFAULT 'video', status TEXT DEFAULT 'draft', duration TEXT, order_index INTEGER DEFAULT 0, watched INTEGER DEFAULT 0, video_url TEXT, local_video_path TEXT, linked_doc_path TEXT, has_quiz INTEGER DEFAULT 0, rating REAL DEFAULT 0.0, rating_count INTEGER DEFAULT 0, FOREIGN KEY(course_id) REFERENCES $TABLE_COURSES(course_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_REFERENCES (ref_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, lesson_id INTEGER NOT NULL, notes TEXT, ai_summary TEXT, FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id), FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ENROLLMENTS (enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, course_id INTEGER NOT NULL, progress INTEGER DEFAULT 0, status TEXT DEFAULT 'in_progress', FOREIGN KEY(student_id) REFERENCES $TABLE_USERS(user_id), FOREIGN KEY(course_id) REFERENCES $TABLE_COURSES(course_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PROJECTS (project_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, student_id INTEGER NOT NULL, related_course_id INTEGER, project_type TEXT DEFAULT 'Individual', semester TEXT, description TEXT, github_repo TEXT, category TEXT, status TEXT DEFAULT 'pending', submitted_date TEXT, is_competition_entry INTEGER DEFAULT 0, video_url TEXT, local_video_path TEXT, linked_doc_path TEXT, rating REAL DEFAULT 0.0, rating_count INTEGER DEFAULT 0, badge TEXT, FOREIGN KEY(student_id) REFERENCES $TABLE_USERS(user_id), FOREIGN KEY(related_course_id) REFERENCES $TABLE_COURSES(course_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_COMMENTS (comment_id INTEGER PRIMARY KEY AUTOINCREMENT, lesson_id INTEGER NOT NULL, user_id INTEGER NOT NULL, user_name TEXT, text TEXT NOT NULL, timestamp TEXT, FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id), FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CONFUSING_REPORTS (report_id INTEGER PRIMARY KEY AUTOINCREMENT, lesson_id INTEGER NOT NULL, user_id INTEGER NOT NULL, timestamp TEXT, FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id), FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (message_id INTEGER PRIMARY KEY AUTOINCREMENT, sender_id INTEGER NOT NULL, receiver_id INTEGER NOT NULL, text TEXT NOT NULL, timestamp TEXT, is_read INTEGER DEFAULT 0, FOREIGN KEY(sender_id) REFERENCES $TABLE_USERS(user_id), FOREIGN KEY(receiver_id) REFERENCES $TABLE_USERS(user_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (notification_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, timestamp TEXT, is_read INTEGER DEFAULT 0, FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_RATINGS (rating_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, entity_id INTEGER NOT NULL, entity_type TEXT NOT NULL, rating_value REAL NOT NULL, comment TEXT, timestamp TEXT, FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_QUIZ_QUESTIONS (question_id INTEGER PRIMARY KEY AUTOINCREMENT, lesson_id INTEGER NOT NULL, question_text TEXT NOT NULL, option_a TEXT NOT NULL, option_b TEXT NOT NULL, option_c TEXT NOT NULL, option_d TEXT NOT NULL, correct_option INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id))")

        if (oldVersion < 2) {
            // Add columns for offline video
            try { db.execSQL("ALTER TABLE $TABLE_LESSONS ADD COLUMN video_url TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_LESSONS ADD COLUMN local_video_path TEXT") } catch (_: Exception) {}
        }
        if (oldVersion < 3) {
            // Add columns for linked docs and quizzes
            try { db.execSQL("ALTER TABLE $TABLE_LESSONS ADD COLUMN linked_doc_path TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_LESSONS ADD COLUMN has_quiz INTEGER DEFAULT 0") } catch (_: Exception) {}
            // Create references table if missing
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_REFERENCES (
                    ref_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    lesson_id INTEGER NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id),
                    FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id)
                )
            """.trimIndent())
        }
        if (oldVersion < 4) {
            // Add columns for Showcase ratings
            try { db.execSQL("ALTER TABLE $TABLE_PROJECTS ADD COLUMN video_url TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_PROJECTS ADD COLUMN rating REAL DEFAULT 0.0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_PROJECTS ADD COLUMN rating_count INTEGER DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_PROJECTS ADD COLUMN badge TEXT") } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            // Create comments and confusing reports tables if missing
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_COMMENTS (
                    comment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lesson_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    user_name TEXT,
                    text TEXT NOT NULL,
                    timestamp TEXT,
                    FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id),
                    FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_CONFUSING_REPORTS (
                    report_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lesson_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    timestamp TEXT,
                    FOREIGN KEY(lesson_id) REFERENCES $TABLE_LESSONS(lesson_id),
                    FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
                )
            """.trimIndent())
        }
        if (oldVersion < 6) {
            // Add lesson rating column
            try { db.execSQL("ALTER TABLE $TABLE_LESSONS ADD COLUMN rating REAL DEFAULT 0.0") } catch (_: Exception) {}
        }
        if (oldVersion < 8) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                    notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    timestamp TEXT,
                    is_read INTEGER DEFAULT 0,
                    FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
                )
            """.trimIndent())
        }
        if (oldVersion < 9) {
            try { db.execSQL("ALTER TABLE $TABLE_LESSONS ADD COLUMN rating_count INTEGER DEFAULT 0") } catch (_: Exception) {}
        }
        if (oldVersion < 10) {
            // Ensure all tables exist without deleting any production data
        }
        if (oldVersion < 11) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_RATINGS (
                    rating_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    entity_id INTEGER NOT NULL,
                    entity_type TEXT NOT NULL,
                    rating_value REAL NOT NULL,
                    timestamp TEXT,
                    FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(user_id)
                )
            """.trimIndent())
        }
        if (oldVersion < 12) {
            try { db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN rating REAL DEFAULT 0.0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN rating_count INTEGER DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_COURSES ADD COLUMN rating REAL DEFAULT 0.0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_COURSES ADD COLUMN rating_count INTEGER DEFAULT 0") } catch (_: Exception) {}
        }
        if (oldVersion < 13) {
            try { db.execSQL("ALTER TABLE $TABLE_RATINGS ADD COLUMN comment TEXT") } catch (_: Exception) {}
        }
        if (oldVersion < 14) {
            try { db.execSQL("ALTER TABLE $TABLE_REFERENCES ADD COLUMN ai_summary TEXT") } catch (_: Exception) {}
        }
        if (oldVersion < 15) {
            try { db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN fcm_token TEXT") } catch (_: Exception) {}
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    /** Populates the database with sample data matching the design mockups. */
    private fun seedData(db: SQLiteDatabase) {
        fun user(name: String, email: String, uni: String, sid: String, role: String, status: String = "active"): Long {
            val cv = ContentValues().apply {
                put("full_name", name)
                put("email", email)
                put("university", uni)
                put("student_id", sid)
                put("role", role)
                put("status", status)
                put("created_at", "2024")
            }
            return db.insert(TABLE_USERS, null, cv)
        }

        user("Clive Xaba", "clivexabscsc@gmail.com", "Stanford University", "10436849", "admin")
        user("Clivexabsc", "clivexabsc@gmail.com", "Stanford University", "STU-4916", "student")
        user("Sphumelele Xaba", "clivexabL@gmail.com", "Stanford University", "10436849", "instructor")
        user("Dr. Smith", "smith@university.edu", "Stanford University", "INS-1001", "instructor")
        user("Dr. Johnson", "johnson@university.edu", "Stanford University", "INS-1002", "instructor")
        user("Dr. Brown", "brown@university.edu", "Stanford University", "INS-1003", "instructor")
        user("John Doe", "john.doe@university.edu", "Stanford University", "STU-1001", "student")
        user("Sarah K.", "sarah.k@university.edu", "Stanford University", "STU-1002", "student")
        user("Demo Student", "student2@gmail.com", "Stanford University", "STU-2002", "student")
        user("Demo Instructor", "instructor2@gmail.com", "Stanford University", "INS-2002", "instructor")
        user("Demo Admin", "admin2@gmail.com", "Stanford University", "ADM-2002", "admin")

        ensureSampleCoursesExist(db)
    }

    private fun ensureSeedUsersExist(db: SQLiteDatabase) {
        fun ensureUser(name: String, email: String, uni: String, sid: String, role: String) {
            db.rawQuery("SELECT user_id FROM $TABLE_USERS WHERE LOWER(email) = LOWER(?)", arrayOf(email)).use { c ->
                if (!c.moveToFirst()) {
                    val cv = ContentValues().apply {
                        put("full_name", name)
                        put("email", email)
                        put("university", uni)
                        put("student_id", sid)
                        put("role", role)
                        put("status", "active")
                        put("created_at", "2024")
                    }
                    db.insert(TABLE_USERS, null, cv)
                }
            }
        }

        ensureUser("Demo Student", "student2@gmail.com", "Stanford University", "STU-2002", "student")
        ensureUser("Demo Instructor", "instructor2@gmail.com", "Stanford University", "INS-2002", "instructor")
        ensureUser("Demo Admin", "admin2@gmail.com", "Stanford University", "ADM-2002", "admin")
    }

    fun ensureSampleCoursesExist(db: SQLiteDatabase) {
        ensureSeedUsersExist(db)
        var count = 0
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_COURSES", null).use { c ->
            if (c.moveToFirst()) count = c.getInt(0)
        }

        if (count == 0) {
            val instructors = mutableListOf<Long>()
            db.rawQuery("SELECT user_id FROM $TABLE_USERS WHERE role = 'instructor'", null).use { c ->
                while (c.moveToNext()) instructors.add(c.getLong(0))
            }
            val defaultInstructorId = instructors.firstOrNull() ?: 3L

            fun addCourse(title: String, code: String, category: String, semester: String, desc: String, instId: Long, tile: String): Long {
                val cv = ContentValues().apply {
                    put("title", title)
                    put("code", code)
                    put("category", category)
                    put("semester", semester)
                    put("description", desc)
                    put("instructor_id", instId)
                    put("status", "published")
                    put("students_count", 24)
                    put("completion_rate", 78)
                    put("tile_color", tile)
                    put("rating", 4.8f)
                    put("rating_count", 12)
                }
                return db.insert(TABLE_COURSES, null, cv)
            }

            val c1 = addCourse("Full Stack Web Development", "CS 501", "Web Dev", "Fall 2025", "Learn HTML, CSS, JavaScript, Node.js, and React to build modern responsive web applications.", defaultInstructorId, "purple")
            val c2 = addCourse("Android App Development with Kotlin", "CS 502", "Mobile Dev", "Spring 2026", "Master modern Android development using Kotlin, Jetpack Compose, Room Database, and Coroutines.", defaultInstructorId, "pink")
            val c3 = addCourse("Database Systems & SQL", "CS 503", "Databases", "Fall 2025", "Understand relational database management systems, complex SQL queries, and data indexing.", defaultInstructorId, "blue")
            val c4 = addCourse("AI/ML Fundamentals", "CS 504", "AI/ML", "Spring 2026", "Explore machine learning algorithms, neural networks, and AI integration for mobile applications.", defaultInstructorId, "orange")

            fun addLesson(courseId: Long, module: String, title: String, type: String, duration: String, orderIdx: Int, url: String? = null, hasQuiz: Boolean = false): Long {
                val cv = ContentValues().apply {
                    put("course_id", courseId)
                    put("module_name", module)
                    put("title", title)
                    put("type", type)
                    put("status", "published")
                    put("duration", duration)
                    put("order_index", orderIdx)
                    put("watched", 0)
                    put("video_url", url)
                    put("has_quiz", if (hasQuiz) 1 else 0)
                    put("rating", 4.9f)
                    put("rating_count", 8)
                }
                return db.insert(TABLE_LESSONS, null, cv)
            }

            val l1 = addLesson(c1, "Module 1: Web Basics", "1. HTML Structure & Semantic Tags", "video", "10:15", 1, "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", true)
            val l2 = addLesson(c1, "Module 1: Web Basics", "2. CSS Flexbox & Grid Layouts", "video", "14:30", 2, "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", false)
            val l3 = addLesson(c1, "Module 2: JavaScript", "3. JavaScript Async/Await & Promises", "video", "18:20", 3, "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", true)

            val l4 = addLesson(c2, "Module 1: Kotlin Intro", "1. Kotlin Syntax & Fundamentals", "video", "12:00", 1, "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoylikes.mp4", true)
            val l5 = addLesson(c2, "Module 1: Kotlin Intro", "2. Jetpack Compose UI Architecture", "video", "22:45", 2, "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", false)

            fun addQuizQuestion(lessonId: Long, qText: String, optA: String, optB: String, optC: String, optD: String, correctIdx: Int) {
                val cv = ContentValues().apply {
                    put("lesson_id", lessonId)
                    put("question_text", qText)
                    put("option_a", optA)
                    put("option_b", optB)
                    put("option_c", optC)
                    put("option_d", optD)
                    put("correct_option", correctIdx)
                }
                db.insert(TABLE_QUIZ_QUESTIONS, null, cv)
            }

            addQuizQuestion(l1, "What does HTML stand for?", "HyperText Markup Language", "HighText Machine Language", "HyperTabular Model Language", "None of the above", 0)
            addQuizQuestion(l1, "Which HTML tag is used for the largest heading?", "<h6>", "<h1>", "<header>", "<heading>", 1)
            addQuizQuestion(l3, "Which keyword is used to handle asynchronous operations in JavaScript?", "async/await", "thread/run", "defer/promise", "sync/hold", 0)
            addQuizQuestion(l4, "Which keyword defines an immutable variable in Kotlin?", "var", "val", "const", "let", 1)

            val students = mutableListOf<Long>()
            db.rawQuery("SELECT user_id FROM $TABLE_USERS WHERE role = 'student'", null).use { c ->
                while (c.moveToNext()) students.add(c.getLong(0))
            }
            for (stId in students) {
                val cvE1 = ContentValues().apply {
                    put("student_id", stId)
                    put("course_id", c1)
                    put("progress", 33)
                    put("status", "in_progress")
                }
                db.insert(TABLE_ENROLLMENTS, null, cvE1)

                val cvE2 = ContentValues().apply {
                    put("student_id", stId)
                    put("course_id", c2)
                    put("progress", 50)
                    put("status", "in_progress")
                }
                db.insert(TABLE_ENROLLMENTS, null, cvE2)
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // USER OPERATIONS
    // ----------------------------------------------------------------------------------

    /** Creates the local profile row for a brand-new Firebase Auth user (email/password or Google). */
    fun createUserProfile(firebaseUid: String, fullName: String, email: String, university: String, studentId: String, role: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("firebase_uid", firebaseUid)
            put("full_name", fullName)
            put("email", email)
            put("university", university)
            put("student_id", studentId)
            put("role", role)
            put("status", "active")
            put("created_at", now())
        }
        return db.insert(TABLE_USERS, null, cv)
    }

    fun isEmailTaken(email: String): Boolean {
        val db = readableDatabase
        db.rawQuery("SELECT user_id FROM $TABLE_USERS WHERE email = ?", arrayOf(email)).use {
            return it.count > 0
        }
    }

    fun updateUserRole(userId: Long, role: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("role", role) }
        db.update(TABLE_USERS, cv, "user_id = ?", arrayOf(userId.toString()))
    }

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        db.rawQuery("SELECT * FROM $TABLE_USERS WHERE LOWER(email) = LOWER(?)", arrayOf(email.trim())).use { c ->
            if (c.moveToFirst()) return c.toUser()
        }
        return null
    }

    /** Looks up the local profile linked to a signed-in Firebase account. Null if this is a brand-new account. */
    fun getUserByFirebaseUid(uid: String): User? {
        val db = readableDatabase
        db.rawQuery("SELECT * FROM $TABLE_USERS WHERE firebase_uid = ?", arrayOf(uid)).use { c ->
            if (c.moveToFirst()) return c.toUser()
        }
        return null
    }

    /** Links an existing-by-email profile (e.g. seeded demo account) to a Firebase account on first login. */
    fun linkFirebaseUidToEmail(email: String, firebaseUid: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("firebase_uid", firebaseUid) }
        db.update(TABLE_USERS, cv, "email = ?", arrayOf(email))
    }

    fun getUserById(id: Long): User? {
        val db = readableDatabase
        db.rawQuery("SELECT * FROM $TABLE_USERS WHERE user_id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) return c.toUser()
        }
        return null
    }

    fun getAllUsers(): List<User> {
        val list = mutableListOf<User>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM $TABLE_USERS ORDER BY user_id DESC", null).use { c ->
            while (c.moveToNext()) list.add(c.toUser())
        }
        return list
    }

    fun searchUsers(query: String): List<User> {
        val list = mutableListOf<User>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE full_name LIKE ? OR email LIKE ? ORDER BY user_id DESC",
            arrayOf("%$query%", "%$query%")
        ).use { c ->
            while (c.moveToNext()) list.add(c.toUser())
        }
        return list
    }

    fun setUserStatus(userId: Long, status: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("status", status) }
        db.update(TABLE_USERS, cv, "user_id = ?", arrayOf(userId.toString()))
    }

    fun updateUser(user: User) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("full_name", user.fullName)
            put("email", user.email)
            put("university", user.university)
            put("student_id", user.studentId)
            put("role", user.role)
            put("status", user.status)
            put("fcm_token", user.fcmToken)
        }
        db.update(TABLE_USERS, cv, "user_id = ?", arrayOf(user.id.toString()))
    }

    fun updateFcmToken(userId: Long, token: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("fcm_token", token) }
        db.update(TABLE_USERS, cv, "user_id = ?", arrayOf(userId.toString()))
    }

    fun deleteUser(userId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Delete all courses created by this instructor (if user is an instructor)
            val courseIds = mutableListOf<Long>()
            db.rawQuery("SELECT course_id FROM $TABLE_COURSES WHERE instructor_id = ?", arrayOf(userId.toString())).use { c ->
                while (c.moveToNext()) {
                    courseIds.add(c.getLong(0))
                }
            }
            for (cId in courseIds) {
                deleteCourseInternal(db, cId)
            }

            // 2. Delete user's user-level records
            db.delete(TABLE_MESSAGES, "sender_id = ? OR receiver_id = ?", arrayOf(userId.toString(), userId.toString()))
            db.delete(TABLE_NOTIFICATIONS, "user_id = ?", arrayOf(userId.toString()))
            db.delete(TABLE_RATINGS, "user_id = ?", arrayOf(userId.toString())) // Clean up ratings made by user
            db.delete(TABLE_ENROLLMENTS, "student_id = ?", arrayOf(userId.toString()))
            db.delete(TABLE_PROJECTS, "student_id = ?", arrayOf(userId.toString()))
            db.delete(TABLE_COMMENTS, "user_id = ?", arrayOf(userId.toString()))
            db.delete(TABLE_REFERENCES, "user_id = ?", arrayOf(userId.toString()))
            db.delete(TABLE_CONFUSING_REPORTS, "user_id = ?", arrayOf(userId.toString()))
            db.delete(TABLE_USERS, "user_id = ?", arrayOf(userId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Cursor.toUser(): User = User(
        id = getLong(getColumnIndexOrThrow("user_id")),
        firebaseUid = getStringOrEmpty("firebase_uid"),
        fullName = getString(getColumnIndexOrThrow("full_name")),
        email = getString(getColumnIndexOrThrow("email")),
        university = getStringOrEmpty("university"),
        studentId = getStringOrEmpty("student_id"),
        role = getString(getColumnIndexOrThrow("role")),
        status = getString(getColumnIndexOrThrow("status")),
        createdAt = getStringOrEmpty("created_at"),
        rating = getFloat(getColumnIndexOrThrow("rating")),
        ratingCount = getInt(getColumnIndexOrThrow("rating_count")),
        fcmToken = getStringOrEmpty("fcm_token")
    )

    // ----------------------------------------------------------------------------------
    // COURSE OPERATIONS
    // ----------------------------------------------------------------------------------

    fun createCourse(course: Course): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", course.title)
            put("code", course.code)
            put("category", course.category)
            put("semester", course.semester)
            put("description", course.description)
            put("instructor_id", course.instructorId)
            put("status", course.status)
            put("students_count", course.studentsCount)
            put("completion_rate", course.completionRate)
            put("tile_color", course.tileColor)
        }
        return db.insert(TABLE_COURSES, null, cv)
    }

    fun getAllCourses(): List<Course> {
        val list = mutableListOf<Course>()
        val db = readableDatabase
        val sql = """
            SELECT c.*, u.full_name AS instructor_name FROM $TABLE_COURSES c
            LEFT JOIN $TABLE_USERS u ON c.instructor_id = u.user_id
            ORDER BY c.course_id DESC
        """.trimIndent()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) list.add(c.toCourse())
        }
        return list
    }

    fun getCoursesByStatus(status: String): List<Course> = getAllCourses().filter { it.status == status }

    fun getCoursesByInstructor(instructorId: Long): List<Course> {
        val list = mutableListOf<Course>()
        val db = readableDatabase
        val sql = """
            SELECT c.*, u.full_name AS instructor_name FROM $TABLE_COURSES c
            LEFT JOIN $TABLE_USERS u ON c.instructor_id = u.user_id
            WHERE c.instructor_id = ?
            ORDER BY c.course_id DESC
        """.trimIndent()
        db.rawQuery(sql, arrayOf(instructorId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toCourse())
        }
        return list
    }

    fun getCourseById(id: Long): Course? = getAllCourses().find { it.id == id }

    fun setCourseStatus(courseId: Long, status: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("status", status) }
        db.update(TABLE_COURSES, cv, "course_id = ?", arrayOf(courseId.toString()))

        // Notify instructor
        getCourseById(courseId)?.let { course ->
            val title = if (status == "published") appContext.getString(R.string.msg_course_approved) else appContext.getString(R.string.msg_course_rejected)
            val msg = if (status == "published") {
                appContext.getString(R.string.msg_course_live_format, course.title)
            } else {
                appContext.getString(R.string.msg_course_rejected_format, course.title)
            }
            addNotification(course.instructorId, title, msg)
        }
    }

    private fun Cursor.toCourse(): Course = Course(
        id = getLong(getColumnIndexOrThrow("course_id")),
        title = getString(getColumnIndexOrThrow("title")),
        code = getStringOrEmpty("code"),
        category = getStringOrEmpty("category"),
        semester = getStringOrEmpty("semester"),
        description = getStringOrEmpty("description"),
        instructorId = getLong(getColumnIndexOrThrow("instructor_id")),
        instructorName = getStringOrEmpty("instructor_name"),
        status = getString(getColumnIndexOrThrow("status")),
        studentsCount = getInt(getColumnIndexOrThrow("students_count")),
        completionRate = getInt(getColumnIndexOrThrow("completion_rate")),
        tileColor = getStringOrEmpty("tile_color").ifEmpty { "purple" },
        rating = getFloat(getColumnIndexOrThrow("rating")),
        ratingCount = getInt(getColumnIndexOrThrow("rating_count"))
    )

    // ----------------------------------------------------------------------------------
    // LESSON OPERATIONS
    // ----------------------------------------------------------------------------------

    fun getLessonsForCourse(courseId: Long): List<Lesson> {
        val list = mutableListOf<Lesson>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT * FROM $TABLE_LESSONS WHERE course_id = ? ORDER BY order_index ASC",
            arrayOf(courseId.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(c.toLesson())
        }
        return list
    }

    fun getDownloadedLessons(): List<Lesson> {
        val list = mutableListOf<Lesson>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT * FROM $TABLE_LESSONS WHERE local_video_path IS NOT NULL AND local_video_path != '' ORDER BY lesson_id DESC",
            null
        ).use { c ->
            while (c.moveToNext()) list.add(c.toLesson())
        }
        return list
    }

    fun addLesson(lesson: Lesson): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("course_id", lesson.courseId)
            put("module_name", lesson.moduleName)
            put("title", lesson.title)
            put("type", lesson.type)
            put("status", lesson.status)
            put("duration", lesson.duration)
            put("order_index", lesson.orderIndex)
            put("watched", if (lesson.watched) 1 else 0)
            put("video_url", lesson.videoUrl)
            put("local_video_path", lesson.localVideoPath)
            put("linked_doc_path", lesson.linkedDocPath)
            put("has_quiz", if (lesson.hasQuiz) 1 else 0)
        }
        return db.insert(TABLE_LESSONS, null, cv)
    }

    fun updateLocalVideoPath(lessonId: Long, path: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("local_video_path", path) }
        db.update(TABLE_LESSONS, cv, "lesson_id = ?", arrayOf(lessonId.toString()))
    }

    fun updateProjectVideoPath(projectId: Long, path: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { 
            put("local_video_path", path)
            put("status", "pending") // Back to pending if edited/new
        }
        db.update(TABLE_PROJECTS, cv, "project_id = ?", arrayOf(projectId.toString()))
    }

    fun updateLessonStatus(lessonId: Long, status: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("status", status) }
        db.update(TABLE_LESSONS, cv, "lesson_id = ?", arrayOf(lessonId.toString()))
    }

    fun markLessonWatched(lessonId: Long) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("watched", 1) }
        db.update(TABLE_LESSONS, cv, "lesson_id = ?", arrayOf(lessonId.toString()))
    }

    fun saveRating(userId: Long, entityId: Long, entityType: String, value: Float, comment: String? = null) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Check if rating exists
            var existingId: Long = -1
            db.rawQuery(
                "SELECT rating_id FROM $TABLE_RATINGS WHERE user_id = ? AND entity_id = ? AND entity_type = ?",
                arrayOf(userId.toString(), entityId.toString(), entityType)
            ).use { c ->
                if (c.moveToFirst()) existingId = c.getLong(0)
            }

            val cv = ContentValues().apply {
                put("user_id", userId)
                put("entity_id", entityId)
                put("entity_type", entityType)
                put("rating_value", value)
                if (comment != null) put("comment", comment)
                put("timestamp", now())
            }

            if (existingId != -1L) {
                db.update(TABLE_RATINGS, cv, "rating_id = ?", arrayOf(existingId.toString()))
            } else {
                db.insert(TABLE_RATINGS, null, cv)
            }

            // Recalculate and update the entity's average field
            updateEntityAverage(db, entityId, entityType)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun updateEntityAverage(db: SQLiteDatabase, entityId: Long, entityType: String) {
        var avg = 0f
        var count = 0
        db.rawQuery(
            "SELECT AVG(rating_value), COUNT(*) FROM $TABLE_RATINGS WHERE entity_id = ? AND entity_type = ?",
            arrayOf(entityId.toString(), entityType)
        ).use { c ->
            if (c.moveToFirst()) {
                avg = c.getFloat(0)
                count = c.getInt(1)
            }
        }

        // Calculate global average C for this entity_type (Bayesian Weighted Rating)
        var globalAvg = 3.0f
        db.rawQuery(
            "SELECT AVG(rating_value) FROM $TABLE_RATINGS WHERE entity_type = ?",
            arrayOf(entityType)
        ).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) {
                globalAvg = c.getFloat(0)
            }
        }

        val minVotes = 1.0f
        val finalRating = if (count > 0) {
            val v = count.toFloat()
            val R = avg
            // Bayesian Weighted Rating formula (IMDb style)
            (v / (v + minVotes) * R) + (minVotes / (v + minVotes) * globalAvg)
        } else {
            0.0f
        }

        when (entityType) {
            "lesson" -> {
                val cv = ContentValues().apply {
                    put("rating", finalRating)
                    put("rating_count", count)
                }
                db.update(TABLE_LESSONS, cv, "lesson_id = ?", arrayOf(entityId.toString()))
            }
            "course" -> {
                val cv = ContentValues().apply {
                    put("rating", finalRating)
                    put("rating_count", count)
                }
                db.update(TABLE_COURSES, cv, "course_id = ?", arrayOf(entityId.toString()))
            }
            "project" -> {
                val cv = ContentValues().apply {
                    put("rating", finalRating)
                    put("rating_count", count)
                }
                db.update(TABLE_PROJECTS, cv, "project_id = ?", arrayOf(entityId.toString()))
            }
            "instructor" -> {
                val cv = ContentValues().apply {
                    put("rating", finalRating)
                    put("rating_count", count)
                }
                db.update(TABLE_USERS, cv, "user_id = ?", arrayOf(entityId.toString()))
            }
        }
    }

    fun rateLesson(lessonId: Long, userId: Long, rating: Float, comment: String? = null) {
        saveRating(userId, lessonId, "lesson", rating, comment)
    }

    fun rateInstructor(instructorId: Long, userId: Long, rating: Float, comment: String? = null) {
        saveRating(userId, instructorId, "instructor", rating, comment)
    }

    fun rateCourse(courseId: Long, userId: Long, rating: Float) {
        saveRating(userId, courseId, "course", rating)
    }

    fun updateLesson(lesson: Lesson, resetRating: Boolean = false) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("module_name", lesson.moduleName)
            put("title", lesson.title)
            put("type", lesson.type)
            put("status", lesson.status)
            put("duration", lesson.duration)
            put("video_url", lesson.videoUrl)
            put("local_video_path", lesson.localVideoPath)
            put("linked_doc_path", lesson.linkedDocPath)
            put("has_quiz", if (lesson.hasQuiz) 1 else 0)
            
            if (resetRating) {
                put("rating", 0.0)
                put("rating_count", 0)
            }
        }
        db.update(TABLE_LESSONS, cv, "lesson_id = ?", arrayOf(lesson.id.toString()))
        
        if (resetRating) {
            // Also delete all existing user ratings for this lesson
            db.delete(TABLE_RATINGS, "entity_id = ? AND entity_type = 'lesson'", arrayOf(lesson.id.toString()))
        }
    }

    fun saveQuizQuestionsForLesson(lessonId: Long, questions: List<QuizQuestion>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_QUIZ_QUESTIONS, "lesson_id = ?", arrayOf(lessonId.toString()))
            for (q in questions) {
                val cv = ContentValues().apply {
                    put("lesson_id", lessonId)
                    put("question_text", q.questionText)
                    put("option_a", q.optionA)
                    put("option_b", q.optionB)
                    put("option_c", q.optionC)
                    put("option_d", q.optionD)
                    put("correct_option", q.correctOption)
                }
                db.insert(TABLE_QUIZ_QUESTIONS, null, cv)
            }
            val cvLesson = ContentValues().apply {
                put("has_quiz", if (questions.isNotEmpty()) 1 else 0)
            }
            db.update(TABLE_LESSONS, cvLesson, "lesson_id = ?", arrayOf(lessonId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getQuizQuestionsForLesson(lessonId: Long): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT * FROM $TABLE_QUIZ_QUESTIONS WHERE lesson_id = ? ORDER BY question_id ASC",
            arrayOf(lessonId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    QuizQuestion(
                        id = c.getLong(c.getColumnIndexOrThrow("question_id")),
                        lessonId = c.getLong(c.getColumnIndexOrThrow("lesson_id")),
                        questionText = c.getString(c.getColumnIndexOrThrow("question_text")),
                        optionA = c.getString(c.getColumnIndexOrThrow("option_a")),
                        optionB = c.getString(c.getColumnIndexOrThrow("option_b")),
                        optionC = c.getString(c.getColumnIndexOrThrow("option_c")),
                        optionD = c.getString(c.getColumnIndexOrThrow("option_d")),
                        correctOption = c.getInt(c.getColumnIndexOrThrow("correct_option"))
                    )
                )
            }
        }
        return list
    }

    fun updateProject(project: Project) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", project.title)
            put("related_course_id", project.relatedCourseId)
            put("project_type", project.projectType)
            put("semester", project.semester)
            put("description", project.description)
            put("github_repo", project.githubRepo)
            put("category", project.category)
            put("status", "pending")
            if (project.linkedDocPath != null) put("linked_doc_path", project.linkedDocPath)
        }
        db.update(TABLE_PROJECTS, cv, "project_id = ?", arrayOf(project.id.toString()))
    }

    fun deleteLesson(lessonId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Delete ratings for this lesson
            db.delete(TABLE_RATINGS, "entity_id = ? AND entity_type = 'lesson'", arrayOf(lessonId.toString()))
            // Delete comments
            db.delete(TABLE_COMMENTS, "lesson_id = ?", arrayOf(lessonId.toString()))
            // Delete report
            db.delete(TABLE_CONFUSING_REPORTS, "lesson_id = ?", arrayOf(lessonId.toString()))
            // Delete references
            db.delete(TABLE_REFERENCES, "lesson_id = ?", arrayOf(lessonId.toString()))
            // Delete quiz questions
            db.delete(TABLE_QUIZ_QUESTIONS, "lesson_id = ?", arrayOf(lessonId.toString()))
            // Delete lesson
            db.delete(TABLE_LESSONS, "lesson_id = ?", arrayOf(lessonId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteCourse(courseId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            deleteCourseInternal(db, courseId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun deleteCourseInternal(db: SQLiteDatabase, courseId: Long) {
        // 1. Get all lesson IDs for this course to clean up their sub-records
        val lessonIds = mutableListOf<String>()
        db.rawQuery("SELECT lesson_id FROM $TABLE_LESSONS WHERE course_id = ?", arrayOf(courseId.toString())).use { c ->
            while (c.moveToNext()) {
                lessonIds.add(c.getLong(0).toString())
            }
        }

        if (lessonIds.isNotEmpty()) {
            val placeholders = lessonIds.joinToString(",") { "?" }
            val idsArray = lessonIds.toTypedArray()
            
            // 2. Delete sub-records for these lessons
            db.delete(TABLE_RATINGS, "entity_id IN ($placeholders) AND entity_type = 'lesson'", idsArray)
            db.delete(TABLE_COMMENTS, "lesson_id IN ($placeholders)", idsArray)
            db.delete(TABLE_CONFUSING_REPORTS, "lesson_id IN ($placeholders)", idsArray)
            db.delete(TABLE_REFERENCES, "lesson_id IN ($placeholders)", idsArray)
            db.delete(TABLE_QUIZ_QUESTIONS, "lesson_id IN ($placeholders)", idsArray)
        }

        // 3. Delete lessons themselves
        db.delete(TABLE_LESSONS, "course_id = ?", arrayOf(courseId.toString()))
        // 4. Delete enrollments
        db.delete(TABLE_ENROLLMENTS, "course_id = ?", arrayOf(courseId.toString()))
        
        // 5. Delete associated projects and their ratings
        val projectIds = mutableListOf<String>()
        db.rawQuery("SELECT project_id FROM $TABLE_PROJECTS WHERE related_course_id = ?", arrayOf(courseId.toString())).use { c ->
            while (c.moveToNext()) {
                projectIds.add(c.getLong(0).toString())
            }
        }
        if (projectIds.isNotEmpty()) {
            val pPlaceholders = projectIds.joinToString(",") { "?" }
            val pIdsArray = projectIds.toTypedArray()
            db.delete(TABLE_RATINGS, "entity_id IN ($pPlaceholders) AND entity_type = 'project'", pIdsArray)
        }
        db.delete(TABLE_PROJECTS, "related_course_id = ?", arrayOf(courseId.toString()))
        
        // 6. Delete course ratings
        db.delete(TABLE_RATINGS, "entity_id = ? AND entity_type = 'course'", arrayOf(courseId.toString()))
        
        // 7. Delete the course
        db.delete(TABLE_COURSES, "course_id = ?", arrayOf(courseId.toString()))
    }

    fun deleteProject(projectId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Delete ratings for this project
            db.delete(TABLE_RATINGS, "entity_id = ? AND entity_type = 'project'", arrayOf(projectId.toString()))
            // Delete project
            db.delete(TABLE_PROJECTS, "project_id = ?", arrayOf(projectId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Cursor.toLesson(): Lesson = Lesson(
        id = getLong(getColumnIndexOrThrow("lesson_id")),
        courseId = getLong(getColumnIndexOrThrow("course_id")),
        moduleName = getStringOrEmpty("module_name"),
        title = getString(getColumnIndexOrThrow("title")),
        type = getStringOrEmpty("type").ifEmpty { "video" },
        status = getStringOrEmpty("status").ifEmpty { "draft" },
        duration = getStringOrEmpty("duration"),
        orderIndex = getInt(getColumnIndexOrThrow("order_index")),
        watched = getInt(getColumnIndexOrThrow("watched")) == 1,
        videoUrl = getStringOrEmpty("video_url"),
        localVideoPath = getStringOrEmpty("local_video_path"),
        linkedDocPath = getStringOrEmpty("linked_doc_path"),
        hasQuiz = getInt(getColumnIndexOrThrow("has_quiz")) == 1,
        rating = getFloat(getColumnIndexOrThrow("rating")),
        ratingCount = getInt(getColumnIndexOrThrow("rating_count"))
    )

    // ----------------------------------------------------------------------------------
    // ENROLLMENT OPERATIONS
    // ----------------------------------------------------------------------------------

    fun enrollStudent(studentId: Long, courseId: Long) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("student_id", studentId)
            put("course_id", courseId)
            put("progress", 0)
            put("status", "in_progress")
        }
        db.insert(TABLE_ENROLLMENTS, null, cv)

        // Fix #6: Increment students_count
        db.execSQL("UPDATE $TABLE_COURSES SET students_count = students_count + 1 WHERE course_id = ?", arrayOf(courseId.toString()))

        // Notify instructor
        val course = getCourseById(courseId)
        val student = getUserById(studentId)
        if (course != null) {
            val title = appContext.getString(R.string.notification_new_enrollment)
            val msg = appContext.getString(
                R.string.msg_enrolled_joined_format,
                student?.fullName ?: appContext.getString(R.string.student_default_name),
                course.title
            )
            addNotification(course.instructorId, title, msg)
        }
    }

    fun getEnrollmentsForStudent(studentId: Long): List<Pair<Enrollment, Course>> {
        val list = mutableListOf<Pair<Enrollment, Course>>()
        val db = readableDatabase
        val sql = """
            SELECT e.*, c.*, u.full_name AS instructor_name FROM $TABLE_ENROLLMENTS e
            JOIN $TABLE_COURSES c ON e.course_id = c.course_id
            LEFT JOIN $TABLE_USERS u ON c.instructor_id = u.user_id
            WHERE e.student_id = ?
        """.trimIndent()
        db.rawQuery(sql, arrayOf(studentId.toString())).use { c ->
            while (c.moveToNext()) {
                val enrollment = Enrollment(
                    id = c.getLong(c.getColumnIndexOrThrow("enrollment_id")),
                    studentId = c.getLong(c.getColumnIndexOrThrow("student_id")),
                    courseId = c.getLong(c.getColumnIndexOrThrow("course_id")),
                    progress = c.getInt(c.getColumnIndexOrThrow("progress")),
                    status = c.getString(c.getColumnIndexOrThrow("status"))
                )
                list.add(enrollment to c.toCourse())
            }
        }
        return list
    }

    fun countEnrolledStudentsForCourse(courseId: Long): Int {
        val db = readableDatabase
        db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_ENROLLMENTS WHERE course_id = ?",
            arrayOf(courseId.toString())
        ).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return 0
    }

    // ----------------------------------------------------------------------------------
    // PROJECT OPERATIONS
    // ----------------------------------------------------------------------------------

    fun createProject(project: Project): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", project.title)
            put("student_id", project.studentId)
            if (project.relatedCourseId > 0) {
                put("related_course_id", project.relatedCourseId)
            } else {
                putNull("related_course_id")
            }
            put("project_type", project.projectType)
            put("semester", project.semester)
            put("description", project.description)
            put("github_repo", project.githubRepo)
            put("category", project.category)
            put("status", "pending")
            put("submitted_date", now())
            put("is_competition_entry", if (project.isCompetitionEntry) 1 else 0)
            put("local_video_path", project.localVideoPath)
            put("linked_doc_path", project.linkedDocPath)
        }
        return db.insert(TABLE_PROJECTS, null, cv)
    }

    fun getAllProjects(): List<Project> {
        val list = mutableListOf<Project>()
        val db = readableDatabase
        val sql = """
            SELECT p.*, u.full_name AS student_name, u.email AS student_email, c.title AS course_title FROM $TABLE_PROJECTS p
            LEFT JOIN $TABLE_USERS u ON p.student_id = u.user_id
            LEFT JOIN $TABLE_COURSES c ON p.related_course_id = c.course_id
            ORDER BY p.project_id DESC
        """.trimIndent()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) list.add(c.toProject())
        }
        return list
    }

    fun getProjectsByStudent(studentId: Long): List<Project> =
        getAllProjects().filter { it.studentId == studentId }

    fun getProjectsByStatus(status: String): List<Project> =
        getAllProjects().filter { it.status == status }

    fun getProjectsForInstructorCourses(instructorId: Long): List<Project> {
        val list = mutableListOf<Project>()
        val db = readableDatabase
        // Use LEFT JOIN for courses so projects with no course (related_course_id = 0) 
        // can still be seen if needed, but normally instructors only manage their course projects.
        // However, if related_course_id is 0, it won't match a course, so we should decide
        // if instructors can see general projects. 
        // Let's use a query that includes projects where the student is enrolled in any of the instructor's courses.
        val sql = """
            SELECT p.*, u.full_name AS student_name, u.email AS student_email, c.title AS course_title FROM $TABLE_PROJECTS p
            LEFT JOIN $TABLE_USERS u ON p.student_id = u.user_id
            LEFT JOIN $TABLE_COURSES c ON p.related_course_id = c.course_id
            WHERE c.instructor_id = ? OR p.related_course_id = 0
            ORDER BY p.project_id DESC
        """.trimIndent()
        db.rawQuery(sql, arrayOf(instructorId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toProject())
        }
        return list
    }

    fun setProjectStatus(projectId: Long, status: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("status", status) }
        db.update(TABLE_PROJECTS, cv, "project_id = ?", arrayOf(projectId.toString()))

        // Notify student
        getAllProjects().find { it.id == projectId }?.let { project ->
            val title = if (status == "approved") appContext.getString(R.string.msg_project_approved) else appContext.getString(R.string.msg_project_rejected)
            val msg = if (status == "approved") {
                appContext.getString(R.string.msg_project_approved_showcase_format, project.title)
            } else {
                appContext.getString(R.string.msg_project_rejected_format, project.title)
            }
            addNotification(project.studentId, title, msg)
        }
    }

    fun rateProject(projectId: Long, userId: Long, stars: Float, badge: String?, comment: String? = null) {
        saveRating(userId, projectId, "project", stars, comment)
        if (badge != null) {
            val db = writableDatabase
            val cv = ContentValues().apply { put("badge", badge) }
            db.update(TABLE_PROJECTS, cv, "project_id = ?", arrayOf(projectId.toString()))
        }
    }

    fun getApprovedShowcaseProjects(): List<Project> {
        val projects = getAllProjects().filter { it.status == "approved" }
        return sortProjectsByWeightedRating(projects)
    }

    fun getCompetitionEntries(): List<Project> {
        val projects = getAllProjects().filter { it.isCompetitionEntry }
        return sortProjectsByWeightedRating(projects)
    }

    private fun sortProjectsByWeightedRating(projects: List<Project>): List<Project> {
        if (projects.isEmpty()) return emptyList()
        val ratedProjects = projects.filter { it.ratingCount > 0 }
        val globalAvg = if (ratedProjects.isNotEmpty()) {
            ratedProjects.map { it.rating }.average().toFloat()
        } else {
            3.0f
        }
        val minVotes = 1.0f // minimum vote threshold

        return projects.sortedWith(compareByDescending<Project> { p ->
            if (p.ratingCount == 0) {
                0.0f
            } else {
                val v = p.ratingCount.toFloat()
                val R = p.rating
                // Bayesian weighted rating formula (IMDb style)
                (v / (v + minVotes) * R) + (minVotes / (v + minVotes) * globalAvg)
            }
        }.thenByDescending { it.ratingCount }
         .thenByDescending { it.rating }
         .thenByDescending { it.id })
    }

    private fun Cursor.toProject(): Project = Project(
        id = getLong(getColumnIndexOrThrow("project_id")),
        title = getString(getColumnIndexOrThrow("title")),
        studentId = getLong(getColumnIndexOrThrow("student_id")),
        studentName = getStringOrEmpty("student_name"),
        studentEmail = getStringOrEmpty("student_email"),
        relatedCourseId = getLong(getColumnIndexOrThrow("related_course_id")),
        relatedCourseName = getStringOrEmpty("course_title"),
        projectType = getStringOrEmpty("project_type").ifEmpty { "Individual" },
        semester = getStringOrEmpty("semester"),
        description = getStringOrEmpty("description"),
        githubRepo = getStringOrEmpty("github_repo"),
        category = getStringOrEmpty("category"),
        status = getString(getColumnIndexOrThrow("status")),
        submittedDate = getStringOrEmpty("submitted_date"),
        isCompetitionEntry = getInt(getColumnIndexOrThrow("is_competition_entry")) == 1,
        videoUrl = getStringOrEmpty("video_url"),
        localVideoPath = getStringOrEmpty("local_video_path"),
        linkedDocPath = getStringOrEmpty("linked_doc_path"),
        rating = getFloat(getColumnIndexOrThrow("rating")),
        ratingCount = getInt(getColumnIndexOrThrow("rating_count")),
        badge = if (isNull(getColumnIndexOrThrow("badge"))) null else getString(getColumnIndexOrThrow("badge"))
    )

    // ----------------------------------------------------------------------------------
    // DASHBOARD COUNTS
    // ----------------------------------------------------------------------------------

    fun countAllUsers(): Int = countRows(TABLE_USERS)
    fun countAllCourses(): Int = countRows(TABLE_COURSES)
    fun countPendingApprovals(): Int {
        val pendingCourses = getCoursesByStatus("pending").size
        val pendingProjects = getProjectsByStatus("pending").size
        return pendingCourses + pendingProjects
    }

    private fun countRows(table: String): Int {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return 0
    }

    // ----------------------------------------------------------------------------------
    // REFERENCE OPERATIONS
    // ----------------------------------------------------------------------------------

    fun addReference(userId: Long, lessonId: Long) {
        val db = writableDatabase
        // Check if already exists to avoid duplicates
        val exists = db.rawQuery(
            "SELECT 1 FROM $TABLE_REFERENCES WHERE user_id = ? AND lesson_id = ?",
            arrayOf(userId.toString(), lessonId.toString())
        ).use { it.moveToFirst() }

        if (!exists) {
            val cv = ContentValues().apply {
                put("user_id", userId)
                put("lesson_id", lessonId)
                put("notes", "")
            }
            db.insert(TABLE_REFERENCES, null, cv)
        }
    }

    fun updateReferenceNotes(userId: Long, lessonId: Long, notes: String, aiSummary: String? = null) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("notes", notes)
            if (aiSummary != null) put("ai_summary", aiSummary)
        }
        db.update(TABLE_REFERENCES, cv, "user_id = ? AND lesson_id = ?", arrayOf(userId.toString(), lessonId.toString()))
    }

    fun deleteReference(userId: Long, lessonId: Long) {
        val db = writableDatabase
        db.delete(TABLE_REFERENCES, "user_id = ? AND lesson_id = ?", arrayOf(userId.toString(), lessonId.toString()))
    }

    fun getReferencesForUser(userId: Long): List<Triple<Lesson, String, String>> {
        val list = mutableListOf<Triple<Lesson, String, String>>()
        val db = readableDatabase
        val sql = """
            SELECT l.*, r.notes, r.ai_summary FROM $TABLE_REFERENCES r
            JOIN $TABLE_LESSONS l ON r.lesson_id = l.lesson_id
            WHERE r.user_id = ?
        """.trimIndent()
        db.rawQuery(sql, arrayOf(userId.toString())).use { c ->
            while (c.moveToNext()) {
                list.add(Triple(
                    c.toLesson(),
                    c.getString(c.getColumnIndexOrThrow("notes")),
                    c.getStringOrEmpty("ai_summary")
                ))
            }
        }
        return list
    }

    fun getReferenceNotes(userId: Long, lessonId: Long): String? {
        val db = readableDatabase
        val sql = "SELECT notes FROM $TABLE_REFERENCES WHERE user_id = ? AND lesson_id = ?"
        db.rawQuery(sql, arrayOf(userId.toString(), lessonId.toString())).use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }

    // ----------------------------------------------------------------------------------
    // PROGRESS OPERATIONS
    // ----------------------------------------------------------------------------------

    enum class CourseProgressResult {
        NONE,
        MILESTONE_50,
        COMPLETED_100
    }

    fun updateCourseProgress(studentId: Long, courseId: Long): CourseProgressResult {
        // Simple Progress: Watch N videos out of total M videos
        val lessons = getLessonsForCourse(courseId)
        val videoLessons = lessons.filter { it.type == "video" && it.status == "published" }
        
        if (videoLessons.isEmpty()) return CourseProgressResult.NONE

        val watchedCount = videoLessons.count { it.watched }
        val newProgress = (watchedCount.toFloat() / videoLessons.size * 100).toInt()

        val db = writableDatabase
        
        // Get old progress to check milestone
        var oldProgress = 0
        db.rawQuery(
            "SELECT progress FROM $TABLE_ENROLLMENTS WHERE student_id = ? AND course_id = ?",
            arrayOf(studentId.toString(), courseId.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                oldProgress = c.getInt(0)
            }
        }

        val cv = ContentValues().apply {
            put("progress", newProgress)
            if (newProgress == 100) {
                put("status", "completed")
            }
        }
        db.update(TABLE_ENROLLMENTS, cv, "student_id = ? AND course_id = ?", arrayOf(studentId.toString(), courseId.toString()))

        return when {
            oldProgress < 100 && newProgress >= 100 -> CourseProgressResult.COMPLETED_100
            oldProgress < 50 && newProgress >= 50 -> CourseProgressResult.MILESTONE_50
            else -> CourseProgressResult.NONE
        }
    }

    // ----------------------------------------------------------------------------------
    // COMMENT & REPORT OPERATIONS
    // ----------------------------------------------------------------------------------

    fun addComment(lessonId: Long, userId: Long, userName: String, text: String, rating: Float = 0f): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("lesson_id", lessonId)
            put("user_id", userId)
            put("user_name", userName)
            put("text", text)
            put("timestamp", now())
        }
        val commentId = db.insert(TABLE_COMMENTS, null, cv)

        // Fix #8: If rating provided, update lesson average and course average
        if (rating > 0) {
            rateLesson(lessonId, userId, rating)
            
            db.rawQuery("SELECT course_id FROM $TABLE_LESSONS WHERE lesson_id = ?", arrayOf(lessonId.toString())).use { c ->
                if (c.moveToFirst()) {
                    val courseId = c.getLong(0)
                    saveRating(userId, courseId, "course", rating)
                }
            }
        }
        return commentId
    }

    fun getInstructorAverageRating(instructorId: Long): Float {
        val db = readableDatabase
        db.rawQuery("SELECT rating FROM $TABLE_USERS WHERE user_id = ?", arrayOf(instructorId.toString())).use { c ->
            if (c.moveToFirst()) return c.getFloat(0)
        }
        return 0f
    }

    fun getAllInteractionsForInstructor(instructorId: Long): List<InstructorInteraction> {
        val list = mutableListOf<InstructorInteraction>()
        val db = readableDatabase
        val sql = """
            SELECT 'comment' as type, c.text, c.timestamp, l.title as lesson_title, COALESCE(u.full_name, c.user_name, 'Student') as user_name, c.user_id, 0.0 as rating, c.comment_id as id
            FROM $TABLE_COMMENTS c
            JOIN $TABLE_LESSONS l ON c.lesson_id = l.lesson_id
            JOIN $TABLE_COURSES co ON l.course_id = co.course_id
            LEFT JOIN $TABLE_USERS u ON c.user_id = u.user_id
            WHERE (co.instructor_id = ? OR co.instructor_id IS NULL OR co.instructor_id = 0 OR NOT EXISTS (SELECT 1 FROM $TABLE_COURSES WHERE instructor_id = ?))
            
            UNION ALL
            
            SELECT 'lesson_rating' as type, r.comment as text, r.timestamp, l.title as lesson_title, COALESCE(u.full_name, 'Student') as user_name, u.user_id, r.rating_value as rating, r.rating_id as id
            FROM $TABLE_RATINGS r
            JOIN $TABLE_LESSONS l ON r.entity_id = l.lesson_id AND r.entity_type = 'lesson'
            JOIN $TABLE_COURSES co ON l.course_id = co.course_id
            LEFT JOIN $TABLE_USERS u ON r.user_id = u.user_id
            WHERE (co.instructor_id = ? OR co.instructor_id IS NULL OR co.instructor_id = 0 OR NOT EXISTS (SELECT 1 FROM $TABLE_COURSES WHERE instructor_id = ?))
            
            UNION ALL
            
            SELECT 'instructor_rating' as type, r.comment as text, r.timestamp, 'Instructor Profile' as lesson_title, COALESCE(u.full_name, 'Student') as user_name, u.user_id, r.rating_value as rating, r.rating_id as id
            FROM $TABLE_RATINGS r
            LEFT JOIN $TABLE_USERS u ON r.user_id = u.user_id
            WHERE r.entity_type = 'instructor' AND r.entity_id = ?
            
            UNION ALL
            
            SELECT 'confusing' as type, 'Marked as confusing' as text, cr.timestamp, l.title as lesson_title, COALESCE(u.full_name, 'Student') as user_name, u.user_id, 0.0 as rating, cr.report_id as id
            FROM $TABLE_CONFUSING_REPORTS cr
            JOIN $TABLE_LESSONS l ON cr.lesson_id = l.lesson_id
            JOIN $TABLE_COURSES co ON l.course_id = co.course_id
            LEFT JOIN $TABLE_USERS u ON cr.user_id = u.user_id
            WHERE (co.instructor_id = ? OR co.instructor_id IS NULL OR co.instructor_id = 0 OR NOT EXISTS (SELECT 1 FROM $TABLE_COURSES WHERE instructor_id = ?))
            
            ORDER BY timestamp DESC
        """.trimIndent()
        
        db.rawQuery(sql, arrayOf(
            instructorId.toString(), instructorId.toString(),
            instructorId.toString(), instructorId.toString(),
            instructorId.toString(),
            instructorId.toString(), instructorId.toString()
        )).use { c ->
            while (c.moveToNext()) {
                list.add(InstructorInteraction(
                    type = c.getString(0),
                    text = c.getString(1) ?: "",
                    timestamp = c.getString(2),
                    lessonTitle = c.getString(3),
                    userName = c.getString(4),
                    userId = c.getLong(5),
                    rating = c.getFloat(6),
                    interactionId = c.getLong(7)
                ))
            }
        }
        return list
    }

    fun getCommentsForLesson(lessonId: Long): List<Comment> {
        val list = mutableListOf<Comment>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM $TABLE_COMMENTS WHERE lesson_id = ? ORDER BY comment_id DESC", arrayOf(lessonId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toComment())
        }
        return list
    }

    fun deleteComment(commentId: Long) {
        val db = writableDatabase
        db.delete(TABLE_COMMENTS, "comment_id = ?", arrayOf(commentId.toString()))
    }

    fun deleteConfusingReport(reportId: Long) {
        val db = writableDatabase
        db.delete(TABLE_CONFUSING_REPORTS, "report_id = ?", arrayOf(reportId.toString()))
    }

    fun deleteRating(ratingId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Get info for recalculation
            var entityId: Long = -1
            var entityType: String = ""
            db.rawQuery("SELECT entity_id, entity_type FROM $TABLE_RATINGS WHERE rating_id = ?", arrayOf(ratingId.toString())).use { c ->
                if (c.moveToFirst()) {
                    entityId = c.getLong(0)
                    entityType = c.getString(1)
                }
            }
            
            db.delete(TABLE_RATINGS, "rating_id = ?", arrayOf(ratingId.toString()))
            
            if (entityId != -1L) {
                updateEntityAverage(db, entityId, entityType)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun reportConfusingContent(lessonId: Long, userId: Long): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("lesson_id", lessonId)
            put("user_id", userId)
            put("timestamp", now())
        }
        return db.insert(TABLE_CONFUSING_REPORTS, null, cv)
    }

    private fun Cursor.toComment(): Comment = Comment(
        id = getLong(getColumnIndexOrThrow("comment_id")),
        lessonId = getLong(getColumnIndexOrThrow("lesson_id")),
        userId = getLong(getColumnIndexOrThrow("user_id")),
        userName = getStringOrEmpty("user_name"),
        text = getString(getColumnIndexOrThrow("text")),
        timestamp = getStringOrEmpty("timestamp")
    )

    // ----------------------------------------------------------------------------------
    // MESSAGE OPERATIONS
    // ----------------------------------------------------------------------------------

    fun sendMessage(senderId: Long, receiverId: Long, text: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("sender_id", senderId)
            put("receiver_id", receiverId)
            put("text", text)
            put("timestamp", now())
            put("is_read", 0)
        }
        val id = db.insert(TABLE_MESSAGES, null, cv)

        // Notify receiver
        val sender = getUserById(senderId)
        val title = appContext.getString(R.string.notification_new_message)
        val msg = appContext.getString(
            R.string.msg_sent_you_message_format,
            sender?.fullName ?: appContext.getString(R.string.label_sender_system)
        )
        addNotification(receiverId, title, msg)

        return id
    }

    fun getMessagesBetween(user1Id: Long, user2Id: Long): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        val db = readableDatabase
        val sql = """
            SELECT * FROM $TABLE_MESSAGES 
            WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
            ORDER BY message_id ASC
        """.trimIndent()
        db.rawQuery(sql, arrayOf(user1Id.toString(), user2Id.toString(), user2Id.toString(), user1Id.toString())).use { c ->
            while (c.moveToNext()) {
                list.add(ChatMessage(
                    id = c.getLong(c.getColumnIndexOrThrow("message_id")),
                    senderId = c.getLong(c.getColumnIndexOrThrow("sender_id")),
                    receiverId = c.getLong(c.getColumnIndexOrThrow("receiver_id")),
                    text = c.getString(c.getColumnIndexOrThrow("text")),
                    timestamp = c.getString(c.getColumnIndexOrThrow("timestamp")),
                    isRead = c.getInt(c.getColumnIndexOrThrow("is_read")) == 1
                ))
            }
        }
        return list
    }

    fun getChatConversations(userId: Long): List<Pair<User, ChatMessage>> {
        val conversations = mutableListOf<Pair<User, ChatMessage>>()
        val db = readableDatabase
        // Get latest message for each unique "other" user
        val sql = """
            SELECT m.*, 
                   CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END as other_id
            FROM $TABLE_MESSAGES m
            WHERE m.sender_id = ? OR m.receiver_id = ?
            GROUP BY other_id
            HAVING m.message_id = MAX(m.message_id)
            ORDER BY m.message_id DESC
        """.trimIndent()

        db.rawQuery(sql, arrayOf(userId.toString(), userId.toString(), userId.toString())).use { c ->
            while (c.moveToNext()) {
                val otherId = c.getLong(c.getColumnIndexOrThrow("other_id"))
                val otherUser = getUserById(otherId)
                if (otherUser != null) {
                    val msg = ChatMessage(
                        id = c.getLong(c.getColumnIndexOrThrow("message_id")),
                        senderId = c.getLong(c.getColumnIndexOrThrow("sender_id")),
                        receiverId = c.getLong(c.getColumnIndexOrThrow("receiver_id")),
                        text = c.getString(c.getColumnIndexOrThrow("text")),
                        timestamp = c.getString(c.getColumnIndexOrThrow("timestamp")),
                        isRead = c.getInt(c.getColumnIndexOrThrow("is_read")) == 1
                    )
                    conversations.add(otherUser to msg)
                }
            }
        }
        return conversations
    }

    fun isStudentEnrolledInCourse(studentId: Long, courseId: Long): Boolean {
        val db = readableDatabase
        db.rawQuery(
            "SELECT 1 FROM $TABLE_ENROLLMENTS WHERE student_id = ? AND course_id = ?",
            arrayOf(studentId.toString(), courseId.toString())
        ).use {
            return it.moveToFirst()
        }
    }

    fun isStudentEnrolledWithInstructor(studentId: Long, instructorId: Long): Boolean {
        val db = readableDatabase
        val sql = """
            SELECT 1 FROM $TABLE_ENROLLMENTS e
            JOIN $TABLE_COURSES c ON e.course_id = c.course_id
            WHERE e.student_id = ? AND c.instructor_id = ?
        """.trimIndent()
        db.rawQuery(sql, arrayOf(studentId.toString(), instructorId.toString())).use {
            return it.moveToFirst()
        }
    }

    // ----------------------------------------------------------------------------------
    // NOTIFICATION OPERATIONS
    // ----------------------------------------------------------------------------------

    fun addNotification(userId: Long, title: String, message: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("user_id", userId)
            put("title", title)
            put("message", message)
            put("timestamp", now())
            put("is_read", 0)
        }
        db.insert(TABLE_NOTIFICATIONS, null, cv)
    }

    fun getNotificationsForUser(userId: Long): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT * FROM $TABLE_NOTIFICATIONS WHERE user_id = ? ORDER BY notification_id DESC",
            arrayOf(userId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(AppNotification(
                    id = c.getLong(c.getColumnIndexOrThrow("notification_id")),
                    userId = c.getLong(c.getColumnIndexOrThrow("user_id")),
                    title = c.getString(c.getColumnIndexOrThrow("title")),
                    message = c.getString(c.getColumnIndexOrThrow("message")),
                    timestamp = c.getString(c.getColumnIndexOrThrow("timestamp")),
                    isRead = c.getInt(c.getColumnIndexOrThrow("is_read")) == 1
                ))
            }
        }
        return list
    }

    fun markNotificationsRead(userId: Long) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("is_read", 1) }
        db.update(TABLE_NOTIFICATIONS, cv, "user_id = ?", arrayOf(userId.toString()))
    }

    fun deleteNotifications(userId: Long) {
        val db = writableDatabase
        db.delete(TABLE_NOTIFICATIONS, "user_id = ?", arrayOf(userId.toString()))
    }

    fun countUnreadNotifications(userId: Long): Int {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_NOTIFICATIONS WHERE user_id = ? AND is_read = 0", arrayOf(userId.toString())).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return 0
    }

    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    private fun Cursor.getStringOrEmpty(col: String): String {
        val idx = getColumnIndex(col)
        return if (idx == -1 || isNull(idx)) "" else getString(idx)
    }

    private fun Cursor.getLongOrZero(col: String): Long {
        val idx = getColumnIndex(col)
        return if (idx == -1 || isNull(idx)) 0L else getLong(idx)
    }
}
