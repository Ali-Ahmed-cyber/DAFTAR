package com.daftar.app.data.registry

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_vaults")
data class ActivityVaultEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val businessTypeId: String,
    val dbFileName: String,
    val countryCode: String,
    val baseCurrency: String,
    val decimalPlaces: Int,
    val fiscalYearStart: String,
    val costingMethodId: String,
    val isArchived: Boolean,
    val recoveryKeyDigest: String,
    val recoveryKeySalt: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "app_preferences")
data class AppPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val languageCode: String = "ar",
    val themeModeId: String = "light",
    val lastActiveVaultId: String? = null,
    val autoLockSeconds: Int = 120,
    val biometricPromptPreferred: Boolean = true,
)
