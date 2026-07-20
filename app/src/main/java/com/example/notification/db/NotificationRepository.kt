package com.example.notification.db

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {

    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun insert(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAll() {
        notificationDao.clearAllNotifications()
    }
}
