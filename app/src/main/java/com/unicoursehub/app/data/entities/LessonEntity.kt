package com.unicoursehub.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["course_id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "lesson_id")
    val id: Long = 0,

    @ColumnInfo(name = "course_id")
    val courseId: Long,

    @ColumnInfo(name = "module_name")
    val moduleName: String? = null,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "type")
    val type: String? = "video",

    @ColumnInfo(name = "status")
    val status: String? = "draft",

    @ColumnInfo(name = "duration")
    val duration: String? = null,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,

    @ColumnInfo(name = "watched")
    val watched: Boolean = false,

    @ColumnInfo(name = "local_video_path")
    val localVideoPath: String? = null,

    @ColumnInfo(name = "video_url")
    val videoUrl: String? = null
)
