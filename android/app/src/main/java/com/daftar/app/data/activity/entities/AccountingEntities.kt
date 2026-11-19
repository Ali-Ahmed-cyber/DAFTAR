package com.daftar.app.data.activity.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "activity_profile")
data class ActivityProfileEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val businessTypeId: String,
    val countryCode: String,
    val baseCurrency: String,
    val decimalPlaces: Int,
    val fiscalYearStart: String,
    val costingMethodId: String,
    val taxRegistrationNo: String = "",
    val phone: String = "",
    val address: String = "",
    val enabledModulesCsv: String = "",
    val logoPrintCaption: String = "DAFTAR",
    val updatedAt: Long,
)

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey val code: String,
    val nameAr: String,
    val nameEn: String,
    val symbol: String,
    val decimalPlaces: Int,
    val exchangeRateToBase: BigDecimal,
    val isBaseCurrency: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "tax_rules")
data class TaxRuleEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val ratePercent: BigDecimal,
    val isCompound: Boolean = false,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val payableAccountId: String = "2102",
    val receivableAccountId: String = "1105",
)

@Entity(tableName = "fiscal_periods")
data class FiscalPeriodEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val startDateIso: String,
    val endDateIso: String,
    val isClosed: Boolean = false,
    val closedAtMillis: Long? = null,
    val closingJournalEntryId: String? = null,
)

@Entity(tableName = "cost_centers")
data class CostCenterEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val parentId: String? = null,
    val isActive: Boolean = true,
)

/**
 * Chart of Accounts node.
 * Deliberately contains NO mutable cached balance field:
 * balances MUST be computed strictly from posted JournalLineEntity rows.
 */
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["code"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val type: String, // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    val normalBalance: String, // DEBIT, CREDIT
    val parentAccountId: String? = null,
    val isGroup: Boolean = false,
    val isSystemProtected: Boolean = true,
    val isActive: Boolean = true,
    val currencyCode: String = "SDG",
)

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["entryNumber"], unique = true),
        Index(value = ["transactionDateIso"]),
        Index(value = ["status"]),
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val entryNumber: String,
    val transactionDateIso: String,
    val recordedAtMillis: Long,
    val description: String,
    val sourceModule: String,
    val sourceDocumentId: String? = null,
    val status: String, // DRAFT, POSTED, REVERSED
    val reversalOfEntryId: String? = null,
    val reversedByEntryId: String? = null,
    val currencyCode: String,
    val exchangeRateAtTx: BigDecimal,
    val fiscalPeriodId: String,
    val createdByUserId: String,
    val isClosingEntry: Boolean = false,
    val isOpeningBalance: Boolean = false,
)

@Entity(
    tableName = "journal_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
    ],
    indices = [
        Index("journalEntryId"),
        Index("accountId"),
        Index("costCenterId"),
        Index("partyId"),
    ]
)
data class JournalLineEntity(
    @PrimaryKey val id: String,
    val journalEntryId: String,
    val lineOrder: Int,
    val accountId: String,
    val debitBase: BigDecimal,
    val creditBase: BigDecimal,
    val originalCurrency: String,
    val originalAmount: BigDecimal,
    val exchangeRateAtTx: BigDecimal,
    val costCenterId: String? = null,
    val branchId: String? = null,
    val partyId: String? = null,
    val memo: String = "",
)
