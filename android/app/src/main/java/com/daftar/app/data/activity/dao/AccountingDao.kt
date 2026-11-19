package com.daftar.app.data.activity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daftar.app.data.activity.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {
    @Query("SELECT * FROM activity_profile LIMIT 1")
    fun observeProfile(): Flow<ActivityProfileEntity?>

    @Query("SELECT * FROM activity_profile LIMIT 1")
    suspend fun getProfile(): ActivityProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ActivityProfileEntity)

    @Query("SELECT * FROM currencies ORDER BY isBaseCurrency DESC, code ASC")
    fun observeCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies ORDER BY isBaseCurrency DESC, code ASC")
    suspend fun getAllCurrencies(): List<CurrencyEntity>

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    suspend fun getCurrency(code: String): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrency(currency: CurrencyEntity)

    @Query("SELECT * FROM tax_rules ORDER BY isDefault DESC, id ASC")
    fun observeTaxRules(): Flow<List<TaxRuleEntity>>

    @Query("SELECT * FROM tax_rules WHERE isActive = 1")
    suspend fun getActiveTaxRules(): List<TaxRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaxRule(rule: TaxRuleEntity)

    @Query("SELECT * FROM fiscal_periods ORDER BY startDateIso DESC")
    fun observeFiscalPeriods(): Flow<List<FiscalPeriodEntity>>

    @Query("SELECT * FROM fiscal_periods ORDER BY startDateIso DESC")
    suspend fun getFiscalPeriods(): List<FiscalPeriodEntity>

    @Query("SELECT * FROM fiscal_periods WHERE id = :id LIMIT 1")
    suspend fun getFiscalPeriodById(id: String): FiscalPeriodEntity?

    @Query("SELECT * FROM fiscal_periods WHERE :dateIso >= startDateIso AND :dateIso <= endDateIso LIMIT 1")
    suspend fun findPeriodForDate(dateIso: String): FiscalPeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFiscalPeriod(period: FiscalPeriodEntity)

    @Query("SELECT * FROM cost_centers ORDER BY code ASC")
    fun observeCostCenters(): Flow<List<CostCenterEntity>>

    @Query("SELECT * FROM cost_centers WHERE isActive = 1 ORDER BY code ASC")
    suspend fun getActiveCostCenters(): List<CostCenterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCostCenter(costCenter: CostCenterEntity)

    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY code ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE code = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Query("SELECT * FROM journal_entries ORDER BY recordedAtMillis DESC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY transactionDateIso ASC, recordedAtMillis ASC")
    suspend fun getAllJournalEntries(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE status IN ('POSTED', 'REVERSED') ORDER BY transactionDateIso ASC, recordedAtMillis ASC")
    suspend fun getPostedJournalEntries(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getJournalEntryById(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Update
    suspend fun updateJournalEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id AND status = 'DRAFT'")
    suspend fun deleteDraftJournalEntry(id: String): Int

    @Query("SELECT * FROM journal_lines ORDER BY lineOrder ASC")
    fun observeAllJournalLines(): Flow<List<JournalLineEntity>>

    @Query("SELECT * FROM journal_lines ORDER BY lineOrder ASC")
    suspend fun getAllJournalLines(): List<JournalLineEntity>

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId ORDER BY lineOrder ASC")
    suspend fun getLinesForEntry(entryId: String): List<JournalLineEntity>

    @Query(
        """
        SELECT jl.* FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.status IN ('POSTED', 'REVERSED')
        ORDER BY je.transactionDateIso ASC, je.recordedAtMillis ASC, jl.lineOrder ASC
        """
    )
    suspend fun getAllPostedLines(): List<JournalLineEntity>

    @Query(
        """
        SELECT jl.* FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.status IN ('POSTED', 'REVERSED') AND jl.accountId = :accountId
        ORDER BY je.transactionDateIso ASC, je.recordedAtMillis ASC, jl.lineOrder ASC
        """
    )
    suspend fun getPostedLinesForAccount(accountId: String): List<JournalLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)
}
