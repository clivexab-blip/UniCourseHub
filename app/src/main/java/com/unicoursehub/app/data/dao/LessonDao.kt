package com.unicoursehub.app.data.dao

import androidx.room.*
import com.unicoursehub.app.data.entities.LessonEntity

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE course_id = :courseId ORDER BY order_index ASC")
    suspend fun getLessonsForCourse(courseId: Long): List<LessonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET watched = :watched WHERE lesson_id = :lessonId")
    suspend fun markLessonWatched(lessonId: Long, watched: Boolean)

    @Query("UPDATE lessons SET local_video_path = :path WHERE lesson_id = :lessonId")
    suspend fun updateLocalVideoPath(lessonId: Long, path: String?)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}
