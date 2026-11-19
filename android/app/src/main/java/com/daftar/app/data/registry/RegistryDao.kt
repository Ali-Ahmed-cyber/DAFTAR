package com.daftar.app.data.registry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistryDao {
    @Query("SELECT * FROM activity_vaults WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeActiveVaults(): Flow<List<ActivityVaultEntity>>

    @Query("SELECT * FROM activity_vaults ORDER BY isArchived ASC, updatedAt DESC")
    fun observeAllVaults(): Flow<List<ActivityVaultEntity>>

    @Query("SELECT * FROM activity_vaults")
    suspend fun getAllVaults(): List<ActivityVaultEntity>

    @Query("SELECT * FROM activity_vaults WHERE id = :vaultId LIMIT 1")
    suspend fun getVaultById(vaultId: String): ActivityVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: ActivityVaultEntity)

    @Update
    suspend fun updateVault(vault: ActivityVaultEntity)

    @Query("UPDATE activity_vaults SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :vaultId")
    suspend fun setArchived(vaultId: String, archived: Boolean, updatedAt: Long)

    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    fun observePreferences(): Flow<AppPreferencesEntity?>

    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferences(): AppPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: AppPreferencesEntity)
}
