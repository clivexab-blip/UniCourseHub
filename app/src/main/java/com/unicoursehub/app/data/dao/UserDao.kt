package com.unicoursehub.app.data.dao

import androidx.room.*
import com.unicoursehub.app.data.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE user_id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE firebase_uid = :uid")
    suspend fun getUserByFirebaseUid(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY user_id DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE full_name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' ORDER BY user_id DESC")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE user_id = :userId")
    suspend fun setUserStatus(userId: Long, status: String)
}
