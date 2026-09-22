package com.unicoursehub.app.data.dao

import androidx.room.*
import com.unicoursehub.app.data.entities.EnrollmentEntity

@Dao
interface EnrollmentDao {
    @Query("SELECT * FROM enrollments WHERE student_id = :studentId")
    suspend fun getEnrollmentsForStudent(studentId: Long): List<EnrollmentEntity>

    @Query("SELECT COUNT(*) FROM enrollments WHERE course_id = :courseId")
    suspend fun countEnrolledStudentsForCourse(courseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity): Long

    @Update
    suspend fun updateEnrollment(enrollment: EnrollmentEntity)

    @Delete
    suspend fun deleteEnrollment(enrollment: EnrollmentEntity)
}
