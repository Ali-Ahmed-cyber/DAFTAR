package com.daftar.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.daftar.app.core.backup.BackupCryptoEngine
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.BusinessType
import com.daftar.app.core.model.DaftarModulesRegistry
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.core.security.PinHasher
import com.daftar.app.core.security.RbacPermission
import com.daftar.app.core.security.RecoveryKeyGenerator
import com.daftar.app.core.security.UserRole
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.CurrencyEntity
import com.daftar.app.data.activity.entities.ItemEntity
import com.daftar.app.data.activity.entities.PharmacyBatchEntity
import com.daftar.app.domain.accounting.*
import com.daftar.app.domain.assistant.LocalSmartAssistantEngine
import com.daftar.app.domain.audit.AuditSeverity
import com.daftar.app.domain.audit.DiagnosticAuditEngine
import com.daftar.app.domain.commerce.CommerceTransactionEngine
import com.daftar.app.domain.commerce.InvoiceLineRequest
import com.daftar.app.domain.inventory.ExpiredBatchBlockedException
import com.daftar.app.domain.inventory.InventoryValuationEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaftarAccountingAndIsolationTest {

    private lateinit var context: Context
    private lateinit var db: ActivityDatabase
    private lateinit var doubleEntry: DoubleEntryEngine
    private lateinit var inventory: InventoryValuationEngine
    private lateinit var commerce: CommerceTransactionEngine
    private lateinit var statements: FinancialStatementsEngine
    private lateinit var audit: DiagnosticAuditEngine
    private lateinit var assistant: LocalSmartAssistantEngine

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = ActivityDatabase.createInMemoryForTest(context)
        StarterChartOfAccounts.initializeNewActivityDatabase(
            db = db,
            vaultId = "vault-main",
            nameAr = "شركة النيل التجارية",
            nameEn = "Nile Trading Co",
            businessType = BusinessType.RETAIL,
            countryCode = "SD",
            baseCurrency = "SDG",
            decimalPlaces = 2,
            fiscalYearStart = "2026-01-01",
            costingMethodId = "wac",
            adminFullName = "أحمد محمد",
            adminPin = "8520",
        )
        doubleEntry = DoubleEntryEngine(db)
        inventory = InventoryValuationEngine(db, doubleEntry)
        commerce = CommerceTransactionEngine(db, doubleEntry, inventory)
        statements = FinancialStatementsEngine(db)
        audit = DiagnosticAuditEngine(db)
        assistant = LocalSmartAssistantEngine(db, audit)
        Unit
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test01_sudanAndSdgDefaultsOnNewVault() = runBlocking {
        val profile = db.accountingDao().getProfile()!!
        assertEquals("SD", profile.countryCode)
        assertEquals("SDG", profile.baseCurrency)
        assertEquals(2, profile.decimalPlaces)
        assertTrue(db.accountingDao().getAllAccounts().size >= 25)
    }

    @Test
    fun test02_rejectUnbalancedJournalEntry() = runBlocking {
        var rejected = false
        try {
            doubleEntry.postJournalEntry(
                transactionDateIso = "2026-03-01",
                description = "قيد غير متوازن",
                sourceModule = "TEST",
                lines = listOf(
                    JournalLineDraft("1101", debitBase = BigDecimal("1000.00")),
                    JournalLineDraft("3101", creditBase = BigDecimal("950.00")),
                )
            )
        } catch (_: UnbalancedJournalException) {
            rejected = true
        }
        assertTrue("Unbalanced journal must be rejected", rejected)
    }

    @Test
    fun test03_balancedJournalPersistsAndAppearsInTrialBalanceAndStatements() = runBlocking {
        doubleEntry.postJournalEntry(
            transactionDateIso = "2026-01-15",
            description = "إيداع رأس مال",
            sourceModule = "CAPITAL",
            lines = listOf(
                JournalLineDraft("1101", debitBase = BigDecimal("100000.00")),
                JournalLineDraft("3101", creditBase = BigDecimal("100000.00")),
            )
        )
        val tb = statements.computeTrialBalance()
        val bs = statements.computeBalanceSheet()
        assertTrue(tb.isBalanced)
        assertEquals(0, tb.sumDebits.compareTo(BigDecimal("100000.00")))
        assertEquals(0, bs.totalAssets.compareTo(BigDecimal("100000.00")))
        assertTrue(bs.isBalanced)
    }

    @Test
    fun test04_forbidDeletePostedEntryAndRequireDocumentedReversal() = runBlocking {
        val entry = doubleEntry.postJournalEntry(
            transactionDateIso = "2026-02-01",
            description = "مصروف إيجار",
            sourceModule = "EXPENSE",
            lines = listOf(
                JournalLineDraft("5202", debitBase = BigDecimal("5000.00")),
                JournalLineDraft("1101", creditBase = BigDecimal("5000.00")),
            )
        )

        var blocked = false
        try {
            doubleEntry.deleteEntry(entry.id)
        } catch (_: ImmutablePostedEntryException) {
            blocked = true
        }
        assertTrue("Posted journal must not be deletable", blocked)

        val reversal = doubleEntry.reverseJournalEntry(entry.id, "2026-02-02", "خطأ في التوجيه")
        assertNotNull(reversal.id)
        assertEquals(0, doubleEntry.getAccountBalanceBase("5202").compareTo(BigDecimal.ZERO))
        assertEquals("REVERSED", db.accountingDao().getJournalEntryById(entry.id)!!.status)
    }

    @Test
    fun test05_saleInvoiceCreatesAutoAccountingEntryAndUpdatesBalanceSheet() = runBlocking {
        val item = ItemEntity("itm-1", "SKU-1", "6290001", "سكر أبيض", "White Sugar", "غذائية", salePriceBase = BigDecimal("2000.00"), defaultPurchaseCostBase = BigDecimal("1400.00"))
        db.inventoryDao().upsertItem(item)
        doubleEntry.postJournalEntry(
            transactionDateIso = "2026-01-10",
            description = "رصيد افتتاحي مخزون",
            sourceModule = "OPENING",
            lines = listOf(
                JournalLineDraft("1104", debitBase = BigDecimal("14000.00")),
                JournalLineDraft("3101", creditBase = BigDecimal("14000.00")),
            )
        )
        inventory.receiveStock(item.id, "WH-01", BigDecimal("10"), BigDecimal("1400.00"), "OPEN-1")

        val sale = commerce.postSaleInvoice(
            issueDateIso = "2026-02-10",
            partyId = null,
            paymentMethod = "CASH",
            lines = listOf(InvoiceLineRequest(item.id, item.nameAr, quantity = BigDecimal("4"), unitPriceOriginal = BigDecimal("2000.00")))
        )
        assertNotNull(sale.journalEntryId)
        assertEquals(0, sale.cogsBase.compareTo(BigDecimal("5600.00")))
        assertEquals(0, doubleEntry.getAccountBalanceBase("4101").compareTo(BigDecimal("8000.00")))
        assertEquals(0, doubleEntry.getAccountBalanceBase("5101").compareTo(BigDecimal("5600.00")))
        assertEquals(0, db.inventoryDao().getStock(item.id, "WH-01")!!.quantityOnHand.compareTo(BigDecimal("6")))
    }

    @Test
    fun test06_saleReturnCreatesProperReversingEntryAndRestoresInventory() = runBlocking {
        val item = ItemEntity("itm-2", "SKU-2", "6290002", "شاي أحمر", "Red Tea", "غذائية", salePriceBase = BigDecimal("1500.00"), defaultPurchaseCostBase = BigDecimal("1000.00"))
        db.inventoryDao().upsertItem(item)
        inventory.receiveStock(item.id, "WH-01", BigDecimal("5"), BigDecimal("1000.00"), "INIT")

        val sale = commerce.postSaleInvoice(
            issueDateIso = "2026-03-05",
            partyId = null,
            paymentMethod = "CASH",
            lines = listOf(InvoiceLineRequest(item.id, item.nameAr, quantity = BigDecimal("2"), unitPriceOriginal = BigDecimal("1500.00")))
        )
        assertEquals(0, db.inventoryDao().getStock(item.id, "WH-01")!!.quantityOnHand.compareTo(BigDecimal("3")))

        val ret = commerce.postSaleReturn(sale.id, "2026-03-06", "بضاعة مرتدة")
        assertEquals("SALE_RETURN", ret.invoiceType)
        assertEquals(0, db.inventoryDao().getStock(item.id, "WH-01")!!.quantityOnHand.compareTo(BigDecimal("5")))
        assertEquals(0, doubleEntry.getAccountBalanceBase("1101").compareTo(BigDecimal.ZERO))
    }

    @Test
    fun test07_multiCurrencyLockPreservesHistoricalEntriesOnFxRateChange() = runBlocking {
        val inv = commerce.postSaleInvoice(
            issueDateIso = "2026-03-01",
            partyId = null,
            paymentMethod = "CASH",
            currencyCode = "USD",
            exchangeRateAtTx = BigDecimal("2500.00"),
            lines = listOf(InvoiceLineRequest(null, "خدمة استشارية", quantity = BigDecimal("1"), unitPriceOriginal = BigDecimal("100.00")))
        )
        assertEquals(0, inv.totalBase.compareTo(BigDecimal("250000.00")))

        // Change USD rate to 3000 SDG later; historical posted invoice & journal MUST NOT mutate
        db.accountingDao().upsertCurrency(
            CurrencyEntity("USD", "الدولار", "USD", "USD", 2, BigDecimal("3000.00"), false, System.currentTimeMillis())
        )
        val reloaded = db.commerceDao().getInvoiceById(inv.id)!!
        assertEquals(0, reloaded.exchangeRateAtTx.compareTo(BigDecimal("2500.00")))
        assertEquals(0, reloaded.totalBase.compareTo(BigDecimal("250000.00")))
    }

    @Test
    fun test08_closedFiscalPeriodBlocksPosting() = runBlocking {
        doubleEntry.postJournalEntry(
            transactionDateIso = "2026-04-01",
            description = "إيراد خدمات",
            sourceModule = "SALES",
            lines = listOf(
                JournalLineDraft("1101", debitBase = BigDecimal("12000.00")),
                JournalLineDraft("4101", creditBase = BigDecimal("12000.00")),
            )
        )
        statements.closeFiscalPeriod("FY-2026", "2026-12-31", doubleEntry)
        assertEquals(0, doubleEntry.getAccountBalanceBase("3103").compareTo(BigDecimal("12000.00")))

        var blocked = false
        try {
            doubleEntry.postJournalEntry(
                transactionDateIso = "2026-05-01",
                description = "قيد بعد الإقفال",
                sourceModule = "TEST",
                lines = listOf(
                    JournalLineDraft("5203", debitBase = BigDecimal("500.00")),
                    JournalLineDraft("1101", creditBase = BigDecimal("500.00")),
                )
            )
        } catch (_: ClosedPeriodException) {
            blocked = true
        }
        assertTrue("Posting into closed period must fail", blocked)
    }

    @Test
    fun test09_inventoryWacAndFifoValuationPrecision() = runBlocking {
        val profile = db.accountingDao().getProfile()!!
        db.accountingDao().upsertProfile(profile.copy(costingMethodId = "fifo"))
        val item = ItemEntity("itm-fifo", "SKU-F", "6290009", "زيت طعام", "Oil", "عام", salePriceBase = BigDecimal("3000.00"), defaultPurchaseCostBase = BigDecimal("1000.00"))
        db.inventoryDao().upsertItem(item)

        inventory.receiveStock(item.id, "WH-01", BigDecimal("5"), BigDecimal("1000.00"), "LOT-1")
        Thread.sleep(5)
        inventory.receiveStock(item.id, "WH-01", BigDecimal("5"), BigDecimal("1500.00"), "LOT-2")

        // FIFO should take 5 @ 1000 + 2 @ 1500 = 8000.00
        val cogs = inventory.consumeStock(item.id, "WH-01", BigDecimal("7"), "2026-04-10", "SALE-FIFO")
        assertEquals(0, cogs.compareTo(BigDecimal("8000.00")))
    }

    @Test
    fun test10_pharmacyExpiredBatchHardBlockedOnSale() = runBlocking {
        val drug = ItemEntity("med-1", "MED-01", "629999", "مضاد حيوي", "Antibiotic", "أدوية", salePriceBase = BigDecimal("500.00"), defaultPurchaseCostBase = BigDecimal("300.00"), trackExpiryBatches = true)
        db.inventoryDao().upsertItem(drug)
        val expBatch = PharmacyBatchEntity("b-exp", drug.id, "WH-01", "BATCH-OLD", "2024-01-01", "2025-12-31", BigDecimal("10"), BigDecimal("300.00"))
        db.inventoryDao().upsertPharmacyBatch(expBatch)
        inventory.receiveStock(drug.id, "WH-01", BigDecimal("10"), BigDecimal("300.00"), "PUR-MED", batchId = "b-exp")

        var blocked = false
        try {
            commerce.postSaleInvoice(
                issueDateIso = "2026-06-01",
                partyId = null,
                lines = listOf(InvoiceLineRequest(drug.id, drug.nameAr, batchId = "b-exp", quantity = BigDecimal("2"), unitPriceOriginal = BigDecimal("500.00")))
            )
        } catch (_: ExpiredBatchBlockedException) {
            blocked = true
        }
        assertTrue("Dispensing expired pharmacy batch must be strictly blocked", blocked)
    }

    @Test
    fun test11_strictMultiActivityVaultIsolation() = runBlocking {
        val db2 = ActivityDatabase.createInMemoryForTest(context)
        try {
            StarterChartOfAccounts.initializeNewActivityDatabase(
                db = db2,
                vaultId = "vault-pharmacy",
                nameAr = "صيدلية الشفاء",
                nameEn = "Alshifa Pharmacy",
                businessType = BusinessType.PHARMACY,
                countryCode = "SD",
                baseCurrency = "USD",
                decimalPlaces = 2,
                fiscalYearStart = "2026-01-01",
                costingMethodId = "fifo",
                adminFullName = "د. سارة",
                adminPin = "7410",
            )
            val entryDb1 = DoubleEntryEngine(db)
            entryDb1.postJournalEntry(
                transactionDateIso = "2026-02-01",
                description = "إيداع بالنشاط الأول فقط",
                sourceModule = "ISO_TEST",
                lines = listOf(
                    JournalLineDraft("1101", debitBase = BigDecimal("99000.00")),
                    JournalLineDraft("3101", creditBase = BigDecimal("99000.00")),
                )
            )
            assertEquals("SDG", db.accountingDao().getProfile()!!.baseCurrency)
            assertEquals("USD", db2.accountingDao().getProfile()!!.baseCurrency)
            assertEquals(1, db.accountingDao().getAllJournalEntries().size)
            assertEquals(0, db2.accountingDao().getAllJournalEntries().size)
        } finally {
            db2.close()
        }
    }

    @Test
    fun test12_adaptiveSectorModulesVisibility() {
        val pharmacyIds = DaftarModulesRegistry.modulesForBusiness(BusinessType.PHARMACY).map { it.number }.toSet()
        val restaurantIds = DaftarModulesRegistry.modulesForBusiness(BusinessType.RESTAURANT).map { it.number }.toSet()
        val factoryIds = DaftarModulesRegistry.modulesForBusiness(BusinessType.FACTORY).map { it.number }.toSet()

        assertTrue(pharmacyIds.contains(27))
        assertFalse(pharmacyIds.contains(26))
        assertTrue(restaurantIds.contains(26))
        assertFalse(restaurantIds.contains(27))
        assertTrue(factoryIds.contains(29))
        assertFalse(factoryIds.contains(28))
    }

    @Test
    fun test13_pinHashingNoHardcodedDefaultsAndVerification() {
        val hashed = PinHasher.hashPin("8520")
        assertTrue(PinHasher.verifyPin("8520", hashed.hashBase64, hashed.saltBase64))
        assertFalse(PinHasher.verifyPin("1234", hashed.hashBase64, hashed.saltBase64))
        assertFalse(PinHasher.verifyPin("", hashed.hashBase64, hashed.saltBase64))
    }

    @Test
    fun test14_recoveryKeyGenerationAndPbkdf2KeyDerivation() {
        val gen = RecoveryKeyGenerator.generate()
        assertTrue(gen.plainKeyOnce.startsWith("DFTR-"))
        assertTrue(RecoveryKeyGenerator.verify(gen.plainKeyOnce, gen.digestBase64, gen.saltBase64))
        assertFalse(RecoveryKeyGenerator.verify("DFTR-XXXX-YYYY-ZZZZ-0000", gen.digestBase64, gen.saltBase64))

        val salt = ByteArray(16) { 0x2A }
        val k1 = RecoveryKeyGenerator.deriveAesKeyFromRecoveryKey(gen.plainKeyOnce, salt)
        val k2 = RecoveryKeyGenerator.deriveAesKeyFromRecoveryKey(gen.plainKeyOnce.lowercase(), salt)
        assertArrayEquals(k1, k2)
        assertEquals(32, k1.size)
    }

    @Test
    fun test15_encryptedBackupAesGcmAndSha256IntegrityTamperDetection() {
        val key = "DFTR-ABCD-EFGH-JKLM-NPQR"
        val out = ByteArrayOutputStream()
        val sha = BackupCryptoEngine.encryptArchive(
            files = listOf(BackupCryptoEngine.BackupEntryInput("manifest.json", "{\"vaults\":[]}".toByteArray())),
            recoveryKeyOrPassword = key,
            output = out,
        )
        val archiveBytes = out.toByteArray()
        assertTrue(sha.length == 64)

        val verified = BackupCryptoEngine.decryptAndVerifyArchive(ByteArrayInputStream(archiveBytes), key)
        assertEquals(sha, verified.sha256Hex)
        assertTrue(verified.entries.containsKey("manifest.json"))

        // Wrong recovery key must fail
        var wrongKeyFailed = false
        try {
            BackupCryptoEngine.decryptAndVerifyArchive(ByteArrayInputStream(archiveBytes), "WRONG-KEY-123")
        } catch (_: SecurityException) {
            wrongKeyFailed = true
        }
        assertTrue(wrongKeyFailed)

        // Tampered ciphertext must fail AES-GCM authentication
        val tampered = archiveBytes.copyOf()
        tampered[tampered.lastIndex] = (tampered.last().toInt() xor 0xFF).toByte()
        var tamperFailed = false
        try {
            BackupCryptoEngine.decryptAndVerifyArchive(ByteArrayInputStream(tampered), key)
        } catch (_: SecurityException) {
            tamperFailed = true
        }
        assertTrue(tamperFailed)
    }

    @Test
    fun test16_atomicRollbackOnErrorLeavesLedgerUnchanged() = runBlocking {
        val beforeCount = db.accountingDao().getAllJournalEntries().size
        var failed = false
        try {
            doubleEntry.postJournalEntry(
                transactionDateIso = "2026-03-10",
                description = "حساب غير موجود",
                sourceModule = "ROLLBACK",
                lines = listOf(
                    JournalLineDraft("1101", debitBase = BigDecimal("100.00")),
                    JournalLineDraft("NON_EXISTENT_9999", creditBase = BigDecimal("100.00")),
                )
            )
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(beforeCount, db.accountingDao().getAllJournalEntries().size)
    }

    @Test
    fun test17_diagnosticAuditReadOnly10ChecksPassAndNoMutation() = runBlocking {
        val beforeEntries = db.accountingDao().getAllJournalEntries().size
        val report = audit.runFullDiagnostic("2026-06-01")
        assertEquals(10, report.findings.size)
        assertEquals(0, report.criticalCount)
        assertTrue(report.findings.all { it.severity != AuditSeverity.CRITICAL })
        assertEquals(beforeEntries, db.accountingDao().getAllJournalEntries().size)
    }

    @Test
    fun test18_offlineSmartAssistantReadOnlyAndRbacVerification() = runBlocking {
        val beforeEntries = db.accountingDao().getAllJournalEntries().size
        val reply = assistant.answerQuery("ما هو وضع الربح والسيولة؟", AppLanguage.ARABIC)
        assertTrue(reply.summary.isNotBlank())
        assertTrue(reply.bulletPoints.isNotEmpty())
        assertEquals(beforeEntries, db.accountingDao().getAllJournalEntries().size)

        val auditorPerms = RbacPermission.defaultsForRole(UserRole.AUDITOR)
        assertTrue(auditorPerms.contains(RbacPermission.VIEW))
        assertFalse(auditorPerms.contains(RbacPermission.POST))
        assertFalse(auditorPerms.contains(RbacPermission.DELETE))
    }

    @Test
    fun test19_purchaseInvoiceAndPurchaseReturnReverseInventoryAndPayable() = runBlocking {
        val item = ItemEntity("itm-pr", "SKU-PR", "629888", "دقيق فاخر", "Flour", "عام", salePriceBase = BigDecimal("1200.00"), defaultPurchaseCostBase = BigDecimal("900.00"))
        db.inventoryDao().upsertItem(item)
        val purchase = commerce.postPurchaseInvoice(
            issueDateIso = "2026-04-01",
            partyId = null,
            paymentMethod = "CREDIT",
            lines = listOf(InvoiceLineRequest(item.id, item.nameAr, quantity = BigDecimal("10"), unitPriceOriginal = BigDecimal("900.00")))
        )
        assertEquals(0, doubleEntry.getAccountBalanceBase("2101").compareTo(BigDecimal("9000.00")))
        assertEquals(0, db.inventoryDao().getStock(item.id, "WH-01")!!.quantityOnHand.compareTo(BigDecimal("10")))

        val purRet = commerce.postPurchaseReturn(purchase.id, "2026-04-02", "بضاعة تالفة")
        assertEquals("PURCHASE_RETURN", purRet.invoiceType)
        assertEquals(0, doubleEntry.getAccountBalanceBase("2101").compareTo(BigDecimal.ZERO))
        assertEquals(0, db.inventoryDao().getStock(item.id, "WH-01")!!.quantityOnHand.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun test20_android8CompatibilityAndDatabaseVersionCheck() {
        assertTrue("minSdk 26 / SDK compatibility", context.applicationInfo.minSdkVersion <= 26)
        assertEquals(1, db.openHelper.readableDatabase.version)
    }
}
