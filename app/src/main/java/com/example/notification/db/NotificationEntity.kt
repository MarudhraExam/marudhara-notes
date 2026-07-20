package com.example.notification.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val receivedTime: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val notificationType: String = "general"
)
