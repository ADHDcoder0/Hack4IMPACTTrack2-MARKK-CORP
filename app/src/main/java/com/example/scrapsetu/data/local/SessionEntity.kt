package com.example.scrapsetu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val email: String,
    val password: String,
    val role: String?,
    val loggedInAt: Long
)
