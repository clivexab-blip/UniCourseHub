package com.unicoursehub.app.data.dao

import androidx.room.*
import com.unicoursehub.app.data.entities.ProjectEntity

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY project_id DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE student_id = :studentId ORDER BY project_id DESC")
    suspend fun getProjectsByStudent(studentId: Long): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY project_id DESC")
    suspend fun getProjectsByStatus(status: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE is_competition_entry = 1 ORDER BY project_id DESC")
    suspend fun getCompetitionEntries(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET status = :status WHERE project_id = :projectId")
    suspend fun setProjectStatus(projectId: Long, status: String)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}
