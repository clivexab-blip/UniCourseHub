package com.unicoursehub.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unicoursehub.app.data.dao.*
import com.unicoursehub.app.data.entities.*

@Database(
    entities = [
        UserEntity::class,
        CourseEntity::class,
        LessonEntity::class,
        EnrollmentEntity::class,
        ProjectEntity::class
    ],
    version = 2, // Incremented version because of the new localVideoPath field and Room migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun projectDao(): ProjectDao

    companion object {
        const val DATABASE_NAME = "unicourse_hub_room.db"
    }
}
