package com.daftar.app.domain.accounting

import androidx.room.withTransaction
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.AccountEntity
import java.math.BigDecimal

data class TrialBalanceRow(
    val account: AccountEntity,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val netDebitBalance: BigDecimal,
    val netCreditBalance: BigDecimal,
)

data class TrialBalanceReport(
    val rows: List<TrialBalanceRow>,
    val sumDebits: BigDecimal,
    val sumCredits: BigDecimal,
    val sumNetDebitBalances: BigDecimal,
    val sumNetCreditBalances: BigDecimal,
    val isBalanced: Boolean,
)

data class IncomeStatementReport(
    val revenueRows: List<Pair<AccountEntity, BigDecimal>>,
    val expenseRows: List<Pair<AccountEntity, BigDecimal>>,
    val totalRevenue: BigDecimal,
    val totalExpense: BigDecimal,
    val netIncome: BigDecimal,
)

data class BalanceSheetReport(
    val assetRows: List<Pair<AccountEntity, BigDecimal>>,
    val liabilityRows: List<Pair<AccountEntity, BigDecimal>>,
    val equityRows: List<Pair<AccountEntity, BigDecimal>>,
    val currentNetIncome: BigDecimal,
    val totalAssets: BigDecimal,
    val totalLiabilitiesAndEquity: BigDecimal,
    val isBalanced: Boolean,
)

data class CashFlowReport(
    val operatingCashFlow: BigDecimal,
    val investingCashFlow: BigDecimal,
    val financingCashFlow: BigDecimal,
    val netCashChange: BigDecimal,
    val endingCashBalance: BigDecimal,
)

class FinancialStatementsEngine(private val db: ActivityDatabase) {

    suspend fun computeTrialBalance(asOfDateIso: String? = null): TrialBalanceReport {
        val accDao = db.accountingDao()
        val accounts = accDao.getAllAccounts().filter { !it.isGroup }
        val validEntryIds = accDao.getPostedJournalEntries()
            .filter { asOfDateIso == null || it.transactionDateIso <= asOfDateIso }
            .map { it.id }
            .toSet()

        val linesByAcc = accDao.getAllPostedLines()
            .filter { it.journalEntryId in validEntryIds }
            .groupBy { it.accountId }

        val rows = accounts.mapNotNull { account ->
            val lines = linesByAcc[account.id].orEmpty()
            val dr = MoneyMath.sumOf(lines.map { it.debitBase })
            val cr = MoneyMath.sumOf(lines.map { it.creditBase })
            if (dr.compareTo(BigDecimal.ZERO) == 0 && cr.compareTo(BigDecimal.ZERO) == 0) {
                return@mapNotNull null
            }
            val diff = dr.subtract(cr)
            val netDr = if (diff > BigDecimal.ZERO) diff else BigDecimal.ZERO
            val netCr = if (diff < BigDecimal.ZERO) diff.abs() else BigDecimal.ZERO
            TrialBalanceRow(account, dr, cr, netDr, netCr)
        }

        val sumDr = MoneyMath.sumOf(rows.map { it.totalDebit })
        val sumCr = MoneyMath.sumOf(rows.map { it.totalCredit })
        val sumNetDr = MoneyMath.sumOf(rows.map { it.netDebitBalance })
        val sumNetCr = MoneyMath.sumOf(rows.map { it.netCreditBalance })

        return TrialBalanceReport(
            rows = rows,
            sumDebits = sumDr,
            sumCredits = sumCr,
            sumNetDebitBalances = sumNetDr,
            sumNetCreditBalances = sumNetCr,
            isBalanced = MoneyMath.equalsExact(sumDr, sumCr) && MoneyMath.equalsExact(sumNetDr, sumNetCr),
        )
    }

    suspend fun computeIncomeStatement(asOfDateIso: String? = null): IncomeStatementReport {
        val tb = computeTrialBalance(asOfDateIso)
        val revenueRows = mutableListOf<Pair<AccountEntity, BigDecimal>>()
        val expenseRows = mutableListOf<Pair<AccountEntity, BigDecimal>>()

        for (row in tb.rows) {
            when (row.account.type) {
                "REVENUE" -> {
                    // Normal credit; contra revenue (sales return/discount) carries debit balance so subtract it
                    val creditNet = row.totalCredit.subtract(row.totalDebit)
                    revenueRows.add(row.account to creditNet)
                }
                "EXPENSE" -> {
                    val debitNet = row.totalDebit.subtract(row.totalCredit)
                    expenseRows.add(row.account to debitNet)
                }
            }
        }

        val totalRev = MoneyMath.sumOf(revenueRows.map { it.second })
        val totalExp = MoneyMath.sumOf(expenseRows.map { it.second })
        return IncomeStatementReport(
            revenueRows = revenueRows,
            expenseRows = expenseRows,
            totalRevenue = totalRev,
            totalExpense = totalExp,
            netIncome = totalRev.subtract(totalExp),
        )
    }

    suspend fun computeBalanceSheet(asOfDateIso: String? = null): BalanceSheetReport {
        val tb = computeTrialBalance(asOfDateIso)
        val pl = computeIncomeStatement(asOfDateIso)

        val assets = mutableListOf<Pair<AccountEntity, BigDecimal>>()
        val liabilities = mutableListOf<Pair<AccountEntity, BigDecimal>>()
        val equity = mutableListOf<Pair<AccountEntity, BigDecimal>>()

        for (row in tb.rows) {
            when (row.account.type) {
                "ASSET" -> assets.add(row.account to row.totalDebit.subtract(row.totalCredit))
                "LIABILITY" -> liabilities.add(row.account to row.totalCredit.subtract(row.totalDebit))
                "EQUITY" -> equity.add(row.account to row.totalCredit.subtract(row.totalDebit))
            }
        }

        val totalAssets = MoneyMath.sumOf(assets.map { it.second })
        val totalLiab = MoneyMath.sumOf(liabilities.map { it.second })
        val totalEq = MoneyMath.sumOf(equity.map { it.second })
        val totalLiabEq = totalLiab.add(totalEq).add(pl.netIncome)

        return BalanceSheetReport(
            assetRows = assets,
            liabilityRows = liabilities,
            equityRows = equity,
            currentNetIncome = pl.netIncome,
            totalAssets = totalAssets,
            totalLiabilitiesAndEquity = totalLiabEq,
            isBalanced = MoneyMath.equalsExact(totalAssets, totalLiabEq),
        )
    }

    suspend fun computeCashFlows(): CashFlowReport {
        val accDao = db.accountingDao()
        val cashAccountIds = setOf("1101", "1102")
        val allEntries = accDao.getPostedJournalEntries().associateBy { it.id }
        val linesByEntry = accDao.getAllPostedLines().groupBy { it.journalEntryId }

        var operating = BigDecimal.ZERO
        var investing = BigDecimal.ZERO
        var financing = BigDecimal.ZERO
        var endingCash = BigDecimal.ZERO

        for ((entryId, lines) in linesByEntry) {
            val cashDelta = lines.filter { it.accountId in cashAccountIds }
                .fold(BigDecimal.ZERO) { acc, l -> acc.add(l.debitBase).subtract(l.creditBase) }
            if (cashDelta.compareTo(BigDecimal.ZERO) == 0) continue

            endingCash = endingCash.add(cashDelta)
            val counterAccounts = lines.filter { it.accountId !in cashAccountIds }.map { it.accountId }
            val entry = allEntries[entryId]
            when {
                counterAccounts.any { it.startsWith("12") } -> investing = investing.add(cashDelta)
                counterAccounts.any { it.startsWith("3") } || entry?.sourceModule == "PARTNERS" -> financing = financing.add(cashDelta)
                else -> operating = operating.add(cashDelta)
            }
        }

        return CashFlowReport(
            operatingCashFlow = operating,
            investingCashFlow = investing,
            financingCashFlow = financing,
            netCashChange = operating.add(investing).add(financing),
            endingCashBalance = endingCash,
        )
    }

    suspend fun closeFiscalPeriod(
        periodId: String,
        closingDateIso: String,
        doubleEntry: DoubleEntryEngine,
        userId: String = "system",
        userName: String = "System",
    ) {
        val accDao = db.accountingDao()
        val period = accDao.getFiscalPeriodById(periodId)
            ?: throw IllegalArgumentException("Period not found: $periodId")
        require(!period.isClosed) { "Period ${period.nameEn} is already closed" }

        val pl = computeIncomeStatement(period.endDateIso)
        val closingDrafts = mutableListOf<JournalLineDraft>()

        for ((acc, netRev) in pl.revenueRows) {
            if (netRev > BigDecimal.ZERO) {
                closingDrafts.add(JournalLineDraft(accountId = acc.id, debitBase = netRev, memo = "إقفال الإيرادات"))
            } else if (netRev < BigDecimal.ZERO) {
                closingDrafts.add(JournalLineDraft(accountId = acc.id, creditBase = netRev.abs(), memo = "إقفال عكس الإيرادات"))
            }
        }
        for ((acc, netExp) in pl.expenseRows) {
            if (netExp > BigDecimal.ZERO) {
                closingDrafts.add(JournalLineDraft(accountId = acc.id, creditBase = netExp, memo = "إقفال المصروفات"))
            } else if (netExp < BigDecimal.ZERO) {
                closingDrafts.add(JournalLineDraft(accountId = acc.id, debitBase = netExp.abs(), memo = "إقفال تخفيض المصروفات"))
            }
        }

        if (pl.netIncome > BigDecimal.ZERO) {
            closingDrafts.add(JournalLineDraft(accountId = "3103", creditBase = pl.netIncome, memo = "ترحيل صافي ربح الفترة"))
        } else if (pl.netIncome < BigDecimal.ZERO) {
            closingDrafts.add(JournalLineDraft(accountId = "3103", debitBase = pl.netIncome.abs(), memo = "ترحيل صافي خسارة الفترة"))
        }

        db.withTransaction {
            val closingEntry = if (closingDrafts.size >= 2) {
                doubleEntry.postJournalEntry(
                    transactionDateIso = closingDateIso,
                    description = "قيد إقفال الفترة المالية ${period.nameAr}",
                    sourceModule = "PERIOD_CLOSING",
                    sourceDocumentId = period.id,
                    lines = closingDrafts,
                    userId = userId,
                    userName = userName,
                    isClosingEntry = true,
                )
            } else null

            accDao.upsertFiscalPeriod(
                period.copy(
                    isClosed = true,
                    closedAtMillis = System.currentTimeMillis(),
                    closingJournalEntryId = closingEntry?.id,
                )
            )
        }
    }
}
