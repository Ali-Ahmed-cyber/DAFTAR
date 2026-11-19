package com.daftar.app.data.activity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daftar.app.data.activity.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GovernanceDao {
    @Query("SELECT * FROM users ORDER BY createdAtMillis ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isActive = 1")
    suspend fun getActiveUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestampMillis DESC")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestampMillis DESC")
    suspend fun getAuditLogs(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun appendAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM local_notifications ORDER BY createdAtMillis DESC")
    fun observeNotifications(): Flow<List<LocalNotificationEntity>>

    @Query("SELECT * FROM local_notifications ORDER BY createdAtMillis DESC")
    suspend fun getNotifications(): List<LocalNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: LocalNotificationEntity)

    @Query("UPDATE local_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE local_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("SELECT * FROM sequence_counters WHERE prefix = :prefix LIMIT 1")
    suspend fun getCounter(prefix: String): SequenceCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: SequenceCounterEntity)
}
