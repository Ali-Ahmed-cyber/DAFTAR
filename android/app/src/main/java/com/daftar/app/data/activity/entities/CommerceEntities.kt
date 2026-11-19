package com.daftar.app.data.activity.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val partyType: String, // CUSTOMER, SUPPLIER, BOTH
    val phone: String = "",
    val address: String = "",
    val taxNumber: String = "",
    val creditLimitBase: BigDecimal = BigDecimal.ZERO,
    val loyaltyPoints: Int = 0,
    val ledgerAccountId: String,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["invoiceType"]),
        Index(value = ["partyId"]),
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val invoiceType: String, // SALE, SALE_RETURN, PURCHASE, PURCHASE_RETURN
    val status: String, // DRAFT, POSTED, REVERSED
    val issueDateIso: String,
    val recordedAtMillis: Long,
    val partyId: String?,
    val branchId: String,
    val warehouseId: String,
    val treasuryAccountId: String,
    val paymentMethod: String, // CASH, BANK, CREDIT
    val currencyCode: String,
    val exchangeRateAtTx: BigDecimal,
    val subtotalOriginal: BigDecimal,
    val discountOriginal: BigDecimal,
    val taxOriginal: BigDecimal,
    val totalOriginal: BigDecimal,
    val subtotalBase: BigDecimal,
    val discountBase: BigDecimal,
    val taxBase: BigDecimal,
    val totalBase: BigDecimal,
    val paidBase: BigDecimal,
    val cogsBase: BigDecimal = BigDecimal.ZERO,
    val journalEntryId: String? = null,
    val reversalOfInvoiceId: String? = null,
    val reversedByInvoiceId: String? = null,
    val notes: String = "",
)

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("invoiceId"), Index("itemId")]
)
data class InvoiceLineEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val itemId: String?,
    val itemNameSnapshot: String,
    val batchId: String? = null,
    val quantity: BigDecimal,
    val unitPriceOriginal: BigDecimal,
    val taxRatePercent: BigDecimal,
    val taxAmountOriginal: BigDecimal,
    val lineTotalOriginal: BigDecimal,
    val lineTotalBase: BigDecimal,
    val unitCostBaseSnapshot: BigDecimal = BigDecimal.ZERO,
)

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey val id: String,
    val voucherNumber: String,
    val voucherType: String, // RECEIPT, PAYMENT, EXPENSE, INCOME, TREASURY_TRANSFER
    val dateIso: String,
    val recordedAtMillis: Long,
    val fromAccountId: String,
    val toAccountId: String,
    val partyId: String? = null,
    val costCenterId: String? = null,
    val currencyCode: String,
    val exchangeRateAtTx: BigDecimal,
    val amountOriginal: BigDecimal,
    val amountBase: BigDecimal,
    val description: String,
    val status: String, // POSTED, REVERSED
    val journalEntryId: String,
)

@Entity(tableName = "treasury_accounts")
data class TreasuryAccountEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val kind: String, // CASH_BOX, BANK
    val linkedGlAccountId: String,
    val currencyCode: String,
    val bankAccountNumber: String = "",
    val lastReconciledDateIso: String? = null,
    val lastStatementBalance: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
)

@Entity(tableName = "bank_reconciliations")
data class BankReconciliationEntity(
    @PrimaryKey val id: String,
    val treasuryAccountId: String,
    val statementDateIso: String,
    val statementBalance: BigDecimal,
    val bookBalanceAtTime: BigDecimal,
    val difference: BigDecimal,
    val notes: String = "",
    val createdAtMillis: Long,
)
