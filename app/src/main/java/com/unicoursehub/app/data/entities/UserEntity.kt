package com.unicoursehub.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["firebase_uid"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val id: Long = 0,

    @ColumnInfo(name = "firebase_uid")
    val firebaseUid: String? = null,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "university")
    val university: String? = null,

    @ColumnInfo(name = "student_id")
    val studentId: String? = null,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "status")
    val status: String = "active",

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null
)
