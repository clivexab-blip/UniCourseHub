package com.unicoursehub.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "enrollments",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["course_id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "enrollment_id")
    val id: Long = 0,

    @ColumnInfo(name = "student_id")
    val studentId: Long,

    @ColumnInfo(name = "course_id")
    val courseId: Long,

    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    @ColumnInfo(name = "status")
    val status: String? = "in_progress"
)
