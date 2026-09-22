package com.unicoursehub.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["instructor_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "course_id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "code")
    val code: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "semester")
    val semester: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "instructor_id")
    val instructorId: Long,

    @ColumnInfo(name = "status")
    val status: String = "pending",

    @ColumnInfo(name = "students_count")
    val studentsCount: Int = 0,

    @ColumnInfo(name = "completion_rate")
    val completionRate: Int = 0,

    @ColumnInfo(name = "tile_color")
    val tileColor: String? = "purple"
)
