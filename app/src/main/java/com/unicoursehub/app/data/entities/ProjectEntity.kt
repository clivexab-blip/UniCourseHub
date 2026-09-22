package com.unicoursehub.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
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
            childColumns = ["related_course_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "project_id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "student_id")
    val studentId: Long,

    @ColumnInfo(name = "related_course_id")
    val relatedCourseId: Long? = null,

    @ColumnInfo(name = "project_type")
    val projectType: String? = "Individual",

    @ColumnInfo(name = "semester")
    val semester: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "github_repo")
    val githubRepo: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "status")
    val status: String? = "pending",

    @ColumnInfo(name = "submitted_date")
    val submittedDate: String? = null,

    @ColumnInfo(name = "is_competition_entry")
    val isCompetitionEntry: Boolean = false
)
