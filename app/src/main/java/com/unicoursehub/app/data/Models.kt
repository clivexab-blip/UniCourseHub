package com.unicoursehub.app.data

/**
 * Data models for UniCourse Hub.
 * These map directly to the SQLite tables created in DatabaseHelper.
 */

data class User(
    val id: Long = 0,
    val firebaseUid: String = "",   // links this profile row to the Firebase Auth account
    val fullName: String,
    val email: String,
    val university: String = "",
    val studentId: String = "",
    val role: String,          // "admin" | "student" | "instructor"
    val status: String = "active", // "active" | "suspended"
    val createdAt: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val fcmToken: String? = null
)

data class Course(
    val id: Long = 0,
    val title: String,
    val code: String,
    val category: String = "",
    val semester: String = "",
    val description: String = "",
    val instructorId: Long,
    val instructorName: String = "",
    val status: String = "pending",  // "published" | "pending" | "rejected"
    val studentsCount: Int = 0,
    val completionRate: Int = 0,
    val tileColor: String = "purple", // purple | pink | blue | orange -> maps to gradient tile
    val rating: Float = 0f,
    val ratingCount: Int = 0
)

data class Lesson(
    val id: Long = 0,
    val courseId: Long,
    val moduleName: String,
    val title: String,
    val type: String = "video",   // video | doc
    val status: String = "draft", // draft | published
    val duration: String = "",
    val orderIndex: Int = 0,
    val watched: Boolean = false,
    val videoUrl: String? = null,
    val localVideoPath: String? = null,
    val linkedDocPath: String? = null,
    val hasQuiz: Boolean = false,
    val rating: Float = 0f,
    val ratingCount: Int = 0
)

data class Enrollment(
    val id: Long = 0,
    val studentId: Long,
    val courseId: Long,
    val progress: Int = 0,
    val status: String = "in_progress" // in_progress | completed | saved
)

data class Project(
    val id: Long = 0,
    val title: String,
    val studentId: Long,
    val studentName: String = "",
    val studentEmail: String = "",
    val relatedCourseId: Long = 0,
    val relatedCourseName: String = "",
    val projectType: String = "Individual",
    val semester: String = "",
    val description: String = "",
    val githubRepo: String = "",
    val category: String = "",
    val status: String = "pending", // pending | approved | rejected
    val submittedDate: String = "",
    val isCompetitionEntry: Boolean = false,
    val videoUrl: String? = null,
    val localVideoPath: String? = null,
    val linkedDocPath: String? = null,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val badge: String? = null
)

data class Comment(
    val id: Long = 0,
    val lessonId: Long,
    val userId: Long,
    val userName: String,
    val text: String,
    val timestamp: String
)

data class ChatMessage(
    val id: Long = 0,
    val senderId: Long,
    val receiverId: Long,
    val text: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class InstructorInteraction(
    val type: String, // "comment" | "lesson_rating" | "instructor_rating" | "confusing"
    val text: String,
    val timestamp: String,
    val lessonTitle: String,
    val userName: String,
    val userId: Long,
    val rating: Float = 0f,
    val interactionId: Long = 0
)

data class AppNotification(
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)
