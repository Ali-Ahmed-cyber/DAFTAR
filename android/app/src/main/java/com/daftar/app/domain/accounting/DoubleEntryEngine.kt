package com.daftar.app.domain.accounting

import androidx.room.withTransaction
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.*
import java.math.BigDecimal
import java.util.UUID

class UnbalancedJournalException(message: String) : IllegalArgumentException(message)
class ClosedPeriodException(message: String) : IllegalStateException(message)
class ImmutablePostedEntryException(message: String) : IllegalStateException(message)

data class JournalLineDraft(
    val accountId: String,
    val debitBase: BigDecimal = BigDecimal.ZERO,
    val creditBase: BigDecimal = BigDecimal.ZERO,
    val originalCurrency: String? = null,
    val originalAmount: BigDecimal? = null,
    val exchangeRateAtTx: BigDecimal? = null,
    val costCenterId: String? = null,
    val branchId: String? = null,
    val partyId: String? = null,
    val memo: String = "",
)

data class LedgerRow(
    val entryId: String,
    val entryNumber: String,
    val transactionDateIso: String,
    val recordedAtMillis: Long,
    val description: String,
    val debitBase: BigDecimal,
    val creditBase: BigDecimal,
    val runningBalanceBase: BigDecimal,
    val currencyCode: String,
    val originalAmount: BigDecimal,
    val exchangeRateAtTx: BigDecimal,
    val status: String,
)

class DoubleEntryEngine(private val db: ActivityDatabase) {

    suspend fun nextSequenceNumber(prefix: String): String {
        val gov = db.governanceDao()
        val current = gov.getCounter(prefix)
        val num = current?.nextNumber ?: 1L
        gov.upsertCounter(SequenceCounterEntity(prefix = prefix, nextNumber = num + 1L))
        return "$prefix-${num.toString().padStart(5, '0')}"
    }

    suspend fun postJournalEntry(
        transactionDateIso: String,
        description: String,
        sourceModule: String,
        sourceDocumentId: String? = null,
        currencyCode: String = "SDG",
        exchangeRateAtTx: BigDecimal = BigDecimal.ONE,
        lines: List<JournalLineDraft>,
        userId: String = "system",
        userName: String = "System",
        allowDraft: Boolean = false,
        isClosingEntry: Boolean = false,
        isOpeningBalance: Boolean = false,
        reversalOfEntryId: String? = null,
    ): JournalEntryEntity {
        val accDao = db.accountingDao()
        val profile = accDao.getProfile()
        val decimals = profile?.decimalPlaces ?: 2

        require(lines.size >= 2) { "Double-entry journal requires at least 2 lines" }
        require(exchangeRateAtTx > BigDecimal.ZERO) { "Exchange rate must be positive" }

        val period = accDao.findPeriodForDate(transactionDateIso)
            ?: accDao.getFiscalPeriods().firstOrNull()
            ?: throw IllegalStateException("No active fiscal period found for $transactionDateIso")

        if (period.isClosed && !isClosingEntry) {
            throw ClosedPeriodException("Fiscal period ${period.nameEn} ($transactionDateIso) is closed")
        }

        // Strict accounting rule: validate every line has non-negative debit OR credit, never both, non-zero
        lines.forEachIndexed { idx, line ->
            require(line.debitBase >= BigDecimal.ZERO && line.creditBase >= BigDecimal.ZERO) {
                "Negative amounts are forbidden on line ${idx + 1}; use debit/credit side"
            }
            val hasDebit = line.debitBase.compareTo(BigDecimal.ZERO) > 0
            val hasCredit = line.creditBase.compareTo(BigDecimal.ZERO) > 0
            require(hasDebit xor hasCredit) {
                "Line ${idx + 1} must carry strictly either a positive debit or a positive credit"
            }
        }

        val totalDebit = MoneyMath.sumOf(lines.map { MoneyMath.scaleMoney(it.debitBase, decimals) })
        val totalCredit = MoneyMath.sumOf(lines.map { MoneyMath.scaleMoney(it.creditBase, decimals) })

        if (!allowDraft && !MoneyMath.equalsExact(totalDebit, totalCredit, decimals)) {
            throw UnbalancedJournalException(
                "Unbalanced journal rejected: DR=${totalDebit.toPlainString()} != CR=${totalCredit.toPlainString()}"
            )
        }

        return db.withTransaction {
            // Verify all target accounts exist, are active, and are posting (non-group) leaf accounts
            for (line in lines) {
                val account = accDao.getAccountById(line.accountId)
                    ?: throw IllegalArgumentException("Unknown account ID: ${line.accountId}")
                require(!account.isGroup) { "Cannot post directly to group account ${account.code}" }
                require(account.isActive) { "Cannot post to inactive account ${account.code}" }
            }

            val entryId = UUID.randomUUID().toString()
            val entryNum = nextSequenceNumber("JE")
            val now = System.currentTimeMillis()
            val entryStatus = if (allowDraft) "DRAFT" else "POSTED"

            val entry = JournalEntryEntity(
                id = entryId,
                entryNumber = entryNum,
                transactionDateIso = transactionDateIso,
                recordedAtMillis = now,
                description = description,
                sourceModule = sourceModule,
                sourceDocumentId = sourceDocumentId,
                status = entryStatus,
                reversalOfEntryId = reversalOfEntryId,
                currencyCode = currencyCode,
                exchangeRateAtTx = MoneyMath.scaleRate(exchangeRateAtTx),
                fiscalPeriodId = period.id,
                createdByUserId = userId,
                isClosingEntry = isClosingEntry,
                isOpeningBalance = isOpeningBalance,
            )

            val entities = lines.mapIndexed { idx, draft ->
                val scaledDr = MoneyMath.scaleMoney(draft.debitBase, decimals)
                val scaledCr = MoneyMath.scaleMoney(draft.creditBase, decimals)
                val baseAmt = if (scaledDr > BigDecimal.ZERO) scaledDr else scaledCr
                val origRate = draft.exchangeRateAtTx ?: exchangeRateAtTx
                val origCurr = draft.originalCurrency ?: currencyCode
                val origAmt = draft.originalAmount ?: if (origCurr == (profile?.baseCurrency ?: "SDG")) {
                    baseAmt
                } else {
                    MoneyMath.divide(baseAmt, origRate, decimals)
                }

                JournalLineEntity(
                    id = UUID.randomUUID().toString(),
                    journalEntryId = entryId,
                    lineOrder = idx + 1,
                    accountId = draft.accountId,
                    debitBase = scaledDr,
                    creditBase = scaledCr,
                    originalCurrency = origCurr,
                    originalAmount = MoneyMath.scaleMoney(origAmt, decimals),
                    exchangeRateAtTx = MoneyMath.scaleRate(origRate),
                    costCenterId = draft.costCenterId,
                    branchId = draft.branchId,
                    partyId = draft.partyId,
                    memo = draft.memo,
                )
            }

            accDao.insertJournalEntry(entry)
            accDao.insertJournalLines(entities)

            db.governanceDao().appendAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    userId = userId,
                    userName = userName,
                    actionType = if (allowDraft) "JOURNAL_DRAFT_CREATED" else "JOURNAL_POSTED",
                    entityType = "JOURNAL_ENTRY",
                    entityId = entry.id,
                    summary = "$entryNum ($entryStatus) DR=${totalDebit.toPlainString()} CR=${totalCredit.toPlainString()} [$sourceModule]",
                )
            )
            entry
        }
    }

    suspend fun postDraftJournalEntry(
        entryId: String,
        userId: String = "system",
        userName: String = "System",
    ): JournalEntryEntity {
        val accDao = db.accountingDao()
        val entry = accDao.getJournalEntryById(entryId)
            ?: throw IllegalArgumentException("Journal entry not found: $entryId")
        require(entry.status == "DRAFT") { "Only DRAFT entries can be posted" }
        val period = accDao.getFiscalPeriodById(entry.fiscalPeriodId)
        if (period?.isClosed == true && !entry.isClosingEntry) {
            throw ClosedPeriodException("Fiscal period ${period.nameEn} is closed")
        }
        val lines = accDao.getLinesForEntry(entryId)
        val decimals = accDao.getProfile()?.decimalPlaces ?: 2
        val totalDr = MoneyMath.sumOf(lines.map { it.debitBase })
        val totalCr = MoneyMath.sumOf(lines.map { it.creditBase })
        if (!MoneyMath.equalsExact(totalDr, totalCr, decimals)) {
            throw UnbalancedJournalException("Draft journal $entryId is unbalanced: DR=$totalDr != CR=$totalCr")
        }

        val updated = entry.copy(status = "POSTED")
        accDao.updateJournalEntry(updated)
        return updated
    }

    suspend fun deleteEntry(entryId: String) {
        val accDao = db.accountingDao()
        val existing = accDao.getJournalEntryById(entryId)
            ?: throw IllegalArgumentException("Entry not found")
        if (existing.status != "DRAFT") {
            throw ImmutablePostedEntryException(
                "Posted or reversed journal entries (${existing.entryNumber}) cannot be deleted; create a reversing entry instead"
            )
        }
        accDao.deleteDraftJournalEntry(entryId)
    }

    suspend fun reverseJournalEntry(
        entryId: String,
        reversalDateIso: String,
        reason: String,
        userId: String = "system",
        userName: String = "System",
    ): JournalEntryEntity {
        val accDao = db.accountingDao()
        val original = accDao.getJournalEntryById(entryId)
            ?: throw IllegalArgumentException("Original entry not found: $entryId")

        if (original.status == "DRAFT") {
            throw IllegalStateException("Draft entries can be deleted instead of reversed")
        }
        if (original.status == "REVERSED" || original.reversedByEntryId != null) {
            throw ImmutablePostedEntryException("Entry ${original.entryNumber} is already reversed")
        }

        val originalLines = accDao.getLinesForEntry(entryId)
        val reversedDrafts = originalLines.map { line ->
            JournalLineDraft(
                accountId = line.accountId,
                debitBase = line.creditBase,
                creditBase = line.debitBase,
                originalCurrency = line.originalCurrency,
                originalAmount = line.originalAmount,
                exchangeRateAtTx = line.exchangeRateAtTx,
                costCenterId = line.costCenterId,
                branchId = line.branchId,
                partyId = line.partyId,
                memo = "عكس القيد ${original.entryNumber}: $reason",
            )
        }

        return db.withTransaction {
            val reversal = postJournalEntry(
                transactionDateIso = reversalDateIso,
                description = "قيد عكسي للقيد ${original.entryNumber} — $reason",
                sourceModule = original.sourceModule,
                sourceDocumentId = original.sourceDocumentId,
                currencyCode = original.currencyCode,
                exchangeRateAtTx = original.exchangeRateAtTx,
                lines = reversedDrafts,
                userId = userId,
                userName = userName,
                reversalOfEntryId = original.id,
            )

            accDao.updateJournalEntry(
                original.copy(
                    status = "REVERSED",
                    reversedByEntryId = reversal.id,
                )
            )
            db.governanceDao().appendAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    userId = userId,
                    userName = userName,
                    actionType = "JOURNAL_REVERSED",
                    entityType = "JOURNAL_ENTRY",
                    entityId = original.id,
                    summary = "Reversed ${original.entryNumber} with ${reversal.entryNumber}: $reason",
                )
            )
            reversal
        }
    }

    suspend fun getAccountBalanceBase(accountId: String): BigDecimal {
        val accDao = db.accountingDao()
        val account = accDao.getAccountById(accountId) ?: return BigDecimal.ZERO
        val lines = accDao.getPostedLinesForAccount(accountId)
        val dr = MoneyMath.sumOf(lines.map { it.debitBase })
        val cr = MoneyMath.sumOf(lines.map { it.creditBase })
        return if (account.normalBalance == "DEBIT") dr.subtract(cr) else cr.subtract(dr)
    }

    suspend fun getGeneralLedger(accountId: String): List<LedgerRow> {
        val accDao = db.accountingDao()
        val account = accDao.getAccountById(accountId) ?: return emptyList()
        val entriesById = accDao.getPostedJournalEntries().associateBy { it.id }
        val lines = accDao.getPostedLinesForAccount(accountId)
        var running = BigDecimal.ZERO
        return lines.mapNotNull { line ->
            val entry = entriesById[line.journalEntryId] ?: return@mapNotNull null
            running = if (account.normalBalance == "DEBIT") {
                running.add(line.debitBase).subtract(line.creditBase)
            } else {
                running.add(line.creditBase).subtract(line.debitBase)
            }
            LedgerRow(
                entryId = entry.id,
                entryNumber = entry.entryNumber,
                transactionDateIso = entry.transactionDateIso,
                recordedAtMillis = entry.recordedAtMillis,
                description = entry.description,
                debitBase = line.debitBase,
                creditBase = line.creditBase,
                runningBalanceBase = running,
                currencyCode = line.originalCurrency,
                originalAmount = line.originalAmount,
                exchangeRateAtTx = line.exchangeRateAtTx,
                status = entry.status,
            )
        }
    }
}
