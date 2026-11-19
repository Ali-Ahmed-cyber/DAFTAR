package com.daftar.app.data.activity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daftar.app.data.activity.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommerceDao {
    @Query("SELECT * FROM parties ORDER BY name ASC")
    fun observeParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getActiveParties(): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: String): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParty(party: PartyEntity)

    @Query("SELECT * FROM invoices ORDER BY recordedAtMillis DESC")
    fun observeInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY recordedAtMillis DESC")
    suspend fun getAllInvoices(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id AND status = 'DRAFT'")
    suspend fun deleteDraftInvoice(id: String): Int

    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId")
    suspend fun getLinesForInvoice(invoiceId: String): List<InvoiceLineEntity>

    @Query("SELECT * FROM invoice_lines")
    suspend fun getAllInvoiceLines(): List<InvoiceLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoiceLines(lines: List<InvoiceLineEntity>)

    @Query("SELECT * FROM vouchers ORDER BY recordedAtMillis DESC")
    fun observeVouchers(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers ORDER BY recordedAtMillis DESC")
    suspend fun getAllVouchers(): List<VoucherEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Query("SELECT * FROM treasury_accounts WHERE isActive = 1 ORDER BY kind ASC, nameAr ASC")
    fun observeTreasuryAccounts(): Flow<List<TreasuryAccountEntity>>

    @Query("SELECT * FROM treasury_accounts WHERE isActive = 1")
    suspend fun getTreasuryAccounts(): List<TreasuryAccountEntity>

    @Query("SELECT * FROM treasury_accounts WHERE id = :id LIMIT 1")
    suspend fun getTreasuryById(id: String): TreasuryAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTreasury(treasury: TreasuryAccountEntity)

    @Query("SELECT * FROM bank_reconciliations ORDER BY createdAtMillis DESC")
    fun observeReconciliations(): Flow<List<BankReconciliationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(rec: BankReconciliationEntity)
}
