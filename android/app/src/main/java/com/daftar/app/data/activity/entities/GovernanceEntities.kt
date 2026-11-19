package com.daftar.app.data.activity.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val username: String,
    val roleId: String,
    val permissionsCsv: String,
    val pinHash: String,
    val pinSalt: String,
    val biometricEnabled: Boolean = true,
    val isActive: Boolean = true,
    val failedAttempts: Int = 0,
    val lockedUntilMillis: Long = 0L,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("timestampMillis"), Index("actionType")]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val userId: String,
    val userName: String,
    val actionType: String,
    val entityType: String,
    val entityId: String,
    val summary: String,
    val isSecurityEvent: Boolean = false,
)

@Entity(tableName = "local_notifications")
data class LocalNotificationEntity(
    @PrimaryKey val id: String,
    val severity: String, // INFO, WARNING, CRITICAL
    val category: String, // EXPIRY, LOW_STOCK, AUDIT, BACKUP, SYSTEM
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val isRead: Boolean = false,
    val createdAtMillis: Long,
)

@Entity(tableName = "sequence_counters")
data class SequenceCounterEntity(
    @PrimaryKey val prefix: String,
    val nextNumber: Long,
)
