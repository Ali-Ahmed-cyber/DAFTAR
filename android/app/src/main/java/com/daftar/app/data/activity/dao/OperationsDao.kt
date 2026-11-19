package com.daftar.app.data.activity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daftar.app.data.activity.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationsDao {
    @Query("SELECT * FROM employees ORDER BY fullName ASC")
    fun observeEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY fullName ASC")
    suspend fun getActiveEmployees(): List<EmployeeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployee(employee: EmployeeEntity)

    @Query("SELECT * FROM payroll_runs ORDER BY createdAtMillis DESC")
    fun observePayrollRuns(): Flow<List<PayrollRunEntity>>

    @Query("SELECT * FROM payroll_runs ORDER BY createdAtMillis DESC")
    suspend fun getPayrollRuns(): List<PayrollRunEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayrollRun(run: PayrollRunEntity)

    @Query("SELECT * FROM fixed_assets ORDER BY purchaseDateIso DESC")
    fun observeFixedAssets(): Flow<List<FixedAssetEntity>>

    @Query("SELECT * FROM fixed_assets ORDER BY purchaseDateIso DESC")
    suspend fun getFixedAssets(): List<FixedAssetEntity>

    @Query("SELECT * FROM fixed_assets WHERE id = :id LIMIT 1")
    suspend fun getFixedAssetById(id: String): FixedAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFixedAsset(asset: FixedAssetEntity)

    @Query("SELECT * FROM budgets")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM attachments ORDER BY createdAtMillis DESC")
    fun observeAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments ORDER BY createdAtMillis DESC")
    suspend fun getAttachments(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)
}
