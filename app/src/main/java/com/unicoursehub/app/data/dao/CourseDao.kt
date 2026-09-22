package com.unicoursehub.app.data.dao

import androidx.room.*
import com.unicoursehub.app.data.entities.CourseEntity

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE course_id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses ORDER BY course_id DESC")
    suspend fun getAllCourses(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE status = :status ORDER BY course_id DESC")
    suspend fun getCoursesByStatus(status: String): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE instructor_id = :instructorId ORDER BY course_id DESC")
    suspend fun getCoursesByInstructor(instructorId: Long): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("UPDATE courses SET status = :status WHERE course_id = :courseId")
    suspend fun setCourseStatus(courseId: Long, status: String)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)
}
