package com.daftar.app.data.activity.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val code: String,
    val fullName: String,
    val jobTitle: String,
    val department: String = "",
    val baseSalaryBase: BigDecimal,
    val advanceBalanceBase: BigDecimal = BigDecimal.ZERO,
    val branchId: String = "BR-01",
    val isActive: Boolean = true,
)

@Entity(tableName = "payroll_runs")
data class PayrollRunEntity(
    @PrimaryKey val id: String,
    val periodLabel: String, // YYYY-MM
    val dateIso: String,
    val totalGrossBase: BigDecimal,
    val totalDeductionsBase: BigDecimal,
    val totalNetBase: BigDecimal,
    val treasuryAccountId: String,
    val journalEntryId: String,
    val createdAtMillis: Long,
)

@Entity(tableName = "fixed_assets")
data class FixedAssetEntity(
    @PrimaryKey val id: String,
    val assetCode: String,
    val nameAr: String,
    val nameEn: String,
    val purchaseDateIso: String,
    val acquisitionCostBase: BigDecimal,
    val salvageValueBase: BigDecimal,
    val usefulLifeMonths: Int,
    val accumulatedDepreciationBase: BigDecimal = BigDecimal.ZERO,
    val assetAccountId: String = "1201",
    val accumDeprAccountId: String = "1209",
    val deprExpenseAccountId: String = "5205",
    val isDisposed: Boolean = false,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val fiscalPeriodId: String,
    val accountId: String,
    val costCenterId: String? = null,
    val plannedAmountBase: BigDecimal,
    val notes: String = "",
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val sha256Hex: String,
    val sizeBytes: Long,
    val linkedEntityType: String, // INVOICE, JOURNAL, VOUCHER, ASSET
    val linkedEntityId: String,
    val storedRelativePath: String,
    val createdAtMillis: Long,
)
