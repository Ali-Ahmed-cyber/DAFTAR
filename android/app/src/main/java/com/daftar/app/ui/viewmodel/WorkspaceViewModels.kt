package com.daftar.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.core.di.ActiveVaultSession
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.BusinessType
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.core.security.PinHasher
import com.daftar.app.core.security.RbacPermission
import com.daftar.app.core.security.UserRole
import com.daftar.app.data.activity.entities.*
import com.daftar.app.domain.accounting.*
import com.daftar.app.domain.assistant.AssistantReply
import com.daftar.app.domain.audit.DiagnosticAuditReport
import com.daftar.app.domain.commerce.InvoiceLineRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class WorkspaceSnapshot(
    val profile: ActivityProfileEntity? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val journals: List<JournalEntryEntity> = emptyList(),
    val journalLines: List<JournalLineEntity> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val taxRules: List<TaxRuleEntity> = emptyList(),
    val fiscalPeriods: List<FiscalPeriodEntity> = emptyList(),
    val costCenters: List<CostCenterEntity> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val vouchers: List<VoucherEntity> = emptyList(),
    val treasuries: List<TreasuryAccountEntity> = emptyList(),
    val reconciliations: List<BankReconciliationEntity> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val warehouses: List<WarehouseEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val stocks: List<ItemStockEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val pharmacyBatches: List<PharmacyBatchEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val payrollRuns: List<PayrollRunEntity> = emptyList(),
    val fixedAssets: List<FixedAssetEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val boms: List<ManufacturingBomEntity> = emptyList(),
    val productionOrders: List<ProductionOrderEntity> = emptyList(),
    val restaurantTables: List<RestaurantTableEntity> = emptyList(),
    val kitchenOrders: List<KitchenOrderEntity> = emptyList(),
    val workshopJobs: List<WorkshopJobEntity> = emptyList(),
    val shippingOrders: List<ShippingOrderEntity> = emptyList(),
    val loyaltyOffers: List<LoyaltyOfferEntity> = emptyList(),
    val partnerStakes: List<PartnerStakeEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val notifications: List<LocalNotificationEntity> = emptyList(),
    val trialBalance: TrialBalanceReport? = null,
    val incomeStatement: IncomeStatementReport? = null,
    val balanceSheet: BalanceSheetReport? = null,
    val cashFlow: CashFlowReport? = null,
    val auditReport: DiagnosticAuditReport? = null,
    val assistantHistory: List<AssistantReply> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val currency: String get() = profile?.baseCurrency ?: "SDG"
    val decimals: Int get() = profile?.decimalPlaces ?: 2
    val businessType: BusinessType get() = BusinessType.fromId(profile?.businessTypeId ?: "retail")

    fun accountBalance(accountId: String): BigDecimal {
        val acc = accounts.firstOrNull { it.id == accountId } ?: return BigDecimal.ZERO
        val postedIds = journals.filter { it.status == "POSTED" || it.status == "REVERSED" }.map { it.id }.toSet()
        val lines = journalLines.filter { it.accountId == accountId && it.journalEntryId in postedIds }
        val dr = MoneyMath.sumOf(lines.map { it.debitBase })
        val cr = MoneyMath.sumOf(lines.map { it.creditBase })
        return if (acc.normalBalance == "DEBIT") dr.subtract(cr) else cr.subtract(dr)
    }

    fun itemStockQty(itemId: String): BigDecimal =
        MoneyMath.sumOf(stocks.filter { it.itemId == itemId }.map { it.quantityOnHand })
}

class WorkspaceViewModel(
    private val sessionFlow: StateFlow<ActiveVaultSession?>,
) : ViewModel() {

    private val _snapshot = MutableStateFlow(WorkspaceSnapshot())
    val snapshot: StateFlow<WorkspaceSnapshot> = _snapshot.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            sessionFlow.collectLatest { session ->
                if (session == null) {
                    _snapshot.value = WorkspaceSnapshot()
                } else {
                    observeVault(session)
                }
            }
        }
    }

    private suspend fun observeVault(session: ActiveVaultSession) {
        val db = session.db
        val accDao = db.accountingDao()
        val comDao = db.commerceDao()
        val invDao = db.inventoryDao()
        val opsDao = db.operationsDao()
        val specDao = db.specializedDao()
        val govDao = db.governanceDao()

        val fsEngine = FinancialStatementsEngine(db)

        // Trigger recompute whenever any major ledger/commerce/inventory flow updates
        combine(
            accDao.observeProfile(),
            accDao.observeAccounts(),
            accDao.observeJournalEntries(),
            accDao.observeAllJournalLines(),
            comDao.observeInvoices(),
        ) { profile, accounts, journals, lines, invoices ->
            listOf(profile, accounts, journals, lines, invoices)
        }.collectLatest {
            refreshAll(session, fsEngine)
        }
    }

    private suspend fun refreshAll(session: ActiveVaultSession, fs: FinancialStatementsEngine) {
        val db = session.db
        val acc = db.accountingDao()
        val com = db.commerceDao()
        val inv = db.inventoryDao()
        val ops = db.operationsDao()
        val spec = db.specializedDao()
        val gov = db.governanceDao()

        _snapshot.update { cur ->
            cur.copy(
                profile = acc.getProfile(),
                accounts = acc.getAllAccounts(),
                journals = acc.getAllJournalEntries(),
                journalLines = acc.getAllJournalLines(),
                currencies = acc.getAllCurrencies(),
                taxRules = acc.getActiveTaxRules(),
                fiscalPeriods = acc.getFiscalPeriods(),
                costCenters = acc.getActiveCostCenters(),
                parties = com.getActiveParties(),
                invoices = com.getAllInvoices(),
                vouchers = com.getAllVouchers(),
                treasuries = com.getTreasuryAccounts(),
                reconciliations = emptyList(),
                branches = inv.getBranches(),
                warehouses = inv.getWarehouses(),
                items = inv.getAllItems(),
                stocks = inv.getAllStocks(),
                stockMovements = inv.getAllStockMovements(),
                pharmacyBatches = inv.getAllPharmacyBatches(),
                employees = ops.getActiveEmployees(),
                payrollRuns = ops.getPayrollRuns(),
                fixedAssets = ops.getFixedAssets(),
                budgets = ops.getBudgets(),
                attachments = ops.getAttachments(),
                projects = spec.getProjects(),
                boms = spec.getBoms(),
                productionOrders = spec.getProductionOrders(),
                restaurantTables = spec.getRestaurantTables(),
                kitchenOrders = spec.getKitchenOrders(),
                workshopJobs = spec.getWorkshopJobs(),
                shippingOrders = spec.getShippingOrders(),
                loyaltyOffers = spec.getLoyaltyOffers(),
                partnerStakes = spec.getPartnerStakes(),
                users = gov.getActiveUsers(),
                auditLogs = gov.getAuditLogs(),
                notifications = gov.getNotifications(),
                trialBalance = fs.computeTrialBalance(),
                incomeStatement = fs.computeIncomeStatement(),
                balanceSheet = fs.computeBalanceSheet(),
                cashFlow = fs.computeCashFlows(),
                auditReport = session.auditEngine.runFullDiagnostic(),
            )
        }
    }

    fun clearFeedback() {
        _snapshot.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    private fun runMutation(successText: String, block: suspend (ActiveVaultSession) -> Unit) {
        val session = sessionFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block(session)
                refreshAll(session, FinancialStatementsEngine(session.db))
                _snapshot.update { it.copy(statusMessage = successText, errorMessage = null) }
            } catch (e: Throwable) {
                _snapshot.update { it.copy(errorMessage = e.message ?: "Operation failed") }
            }
        }
    }

    fun addAccount(code: String, nameAr: String, nameEn: String, type: String, parentId: String?) {
        runMutation("تمت إضافة الحساب $code") { s ->
            val normal = if (type == "ASSET" || type == "EXPENSE") "DEBIT" else "CREDIT"
            s.db.accountingDao().upsertAccount(
                AccountEntity(
                    id = code.trim(),
                    code = code.trim(),
                    nameAr = nameAr.trim(),
                    nameEn = nameEn.ifBlank { nameAr }.trim(),
                    type = type,
                    normalBalance = normal,
                    parentAccountId = parentId,
                    isGroup = false,
                    isSystemProtected = false,
                    currencyCode = _snapshot.value.currency,
                )
            )
        }
    }

    fun postManualJournal(
        dateIso: String,
        description: String,
        debitAccountId: String,
        creditAccountId: String,
        amount: BigDecimal,
        currencyCode: String,
        exchangeRate: BigDecimal,
        asDraft: Boolean,
        costCenterId: String? = null,
    ) {
        runMutation(if (asDraft) "تم حفظ القيد كمسودة" else "تم ترحيل القيد المحاسبي") { s ->
            val baseAmount = MoneyMath.multiply(amount, exchangeRate, _snapshot.value.decimals)
            s.doubleEntry.postJournalEntry(
                transactionDateIso = dateIso,
                description = description,
                sourceModule = "MANUAL_JOURNAL",
                currencyCode = currencyCode,
                exchangeRateAtTx = exchangeRate,
                lines = listOf(
                    JournalLineDraft(debitAccountId, debitBase = baseAmount, originalCurrency = currencyCode, originalAmount = amount, exchangeRateAtTx = exchangeRate, costCenterId = costCenterId),
                    JournalLineDraft(creditAccountId, creditBase = baseAmount, originalCurrency = currencyCode, originalAmount = amount, exchangeRateAtTx = exchangeRate, costCenterId = costCenterId),
                ),
                userId = s.authenticatedUser?.id ?: "system",
                userName = s.authenticatedUser?.fullName ?: "System",
                allowDraft = asDraft,
            )
        }
    }

    fun postDraftJournal(entryId: String) {
        runMutation("تم ترحيل القيد") { s ->
            s.doubleEntry.postDraftJournalEntry(entryId, s.authenticatedUser?.id ?: "system", s.authenticatedUser?.fullName ?: "System")
        }
    }

    fun reverseJournal(entryId: String, reason: String) {
        runMutation("تم إنشاء قيد عكسي موثق") { s ->
            s.doubleEntry.reverseJournalEntry(entryId, LocalDate.now().toString(), reason, s.authenticatedUser?.id ?: "system", s.authenticatedUser?.fullName ?: "System")
        }
    }

    fun deleteJournal(entryId: String) {
        runMutation("تم حذف المسودة") { s ->
            s.doubleEntry.deleteEntry(entryId)
        }
    }

    fun closePeriod(periodId: String) {
        runMutation("تم إقفال الفترة المالية وترحيل صافي الدخل") { s ->
            FinancialStatementsEngine(s.db).closeFiscalPeriod(
                periodId = periodId,
                closingDateIso = LocalDate.now().toString(),
                doubleEntry = s.doubleEntry,
                userId = s.authenticatedUser?.id ?: "system",
                userName = s.authenticatedUser?.fullName ?: "System",
            )
        }
    }

    fun addItem(
        sku: String,
        barcode: String,
        nameAr: String,
        nameEn: String,
        salePrice: BigDecimal,
        costPrice: BigDecimal,
        initialStockQty: BigDecimal,
        trackExpiry: Boolean,
        batchNumber: String = "",
        expiryDateIso: String = "",
    ) {
        runMutation("تم تسجيل الصنف واستلام الرصيد الافتتاحي") { s ->
            val id = UUID.randomUUID().toString()
            val item = ItemEntity(
                id = id,
                sku = sku.ifBlank { "SKU-${System.currentTimeMillis() % 10000}" },
                barcode = barcode.ifBlank { "629${System.currentTimeMillis() % 10000000}" },
                nameAr = nameAr,
                nameEn = nameEn.ifBlank { nameAr },
                categoryName = "عام",
                salePriceBase = salePrice,
                defaultPurchaseCostBase = costPrice,
                trackExpiryBatches = trackExpiry,
            )
            s.db.inventoryDao().upsertItem(item)
            if (initialStockQty > BigDecimal.ZERO) {
                val whId = _snapshot.value.warehouses.firstOrNull()?.id ?: "WH-01"
                val totalVal = MoneyMath.multiply(initialStockQty, costPrice, _snapshot.value.decimals)
                val je = s.doubleEntry.postJournalEntry(
                    transactionDateIso = LocalDate.now().toString(),
                    description = "رصيد افتتاحي مخزني: $nameAr",
                    sourceModule = "OPENING_STOCK",
                    lines = listOf(
                        JournalLineDraft("1104", debitBase = totalVal),
                        JournalLineDraft("3101", creditBase = totalVal),
                    ),
                    isOpeningBalance = true,
                )
                s.inventoryEngine.receiveStock(
                    itemId = id,
                    warehouseId = whId,
                    quantity = initialStockQty,
                    unitCostBase = costPrice,
                    sourceRef = "OPEN-$sku",
                    journalEntryId = je.id,
                    expiryDateIso = expiryDateIso.ifBlank { null },
                    batchNumber = batchNumber.ifBlank { null },
                )
            }
        }
    }

    fun addParty(name: String, partyType: String, phone: String) {
        runMutation("تم حفظ بطاقة $name") { s ->
            val ledgerAcc = if (partyType == "SUPPLIER") "2101" else "1103"
            s.db.commerceDao().upsertParty(
                PartyEntity(
                    id = UUID.randomUUID().toString(),
                    code = "P-${System.currentTimeMillis() % 10000}",
                    name = name,
                    partyType = partyType,
                    phone = phone,
                    ledgerAccountId = ledgerAcc,
                )
            )
        }
    }

    fun issueSaleInvoice(
        partyId: String?,
        paymentMethod: String,
        currencyCode: String,
        exchangeRate: BigDecimal,
        discountOrig: BigDecimal,
        taxRate: BigDecimal,
        lines: List<InvoiceLineRequest>,
    ) {
        runMutation("تم إصدار وترحيل فاتورة المبيعات والقيد المحاسبي") { s ->
            val whId = _snapshot.value.warehouses.firstOrNull()?.id ?: "WH-01"
            val brId = _snapshot.value.branches.firstOrNull()?.id ?: "BR-01"
            val trId = if (paymentMethod == "BANK") "TR-BANK-MAIN" else "TR-CASH-MAIN"
            s.commerceEngine.postSaleInvoice(
                issueDateIso = LocalDate.now().toString(),
                partyId = partyId,
                branchId = brId,
                warehouseId = whId,
                treasuryAccountId = trId,
                paymentMethod = paymentMethod,
                currencyCode = currencyCode,
                exchangeRateAtTx = exchangeRate,
                discountOriginal = discountOrig,
                lines = lines.map { it.copy(taxRatePercent = taxRate) },
                userId = s.authenticatedUser?.id ?: "system",
                userName = s.authenticatedUser?.fullName ?: "System",
            )
        }
    }

    fun returnSaleInvoice(invoiceId: String, reason: String) {
        runMutation("تم إنشاء مرتجع المبيعات وعكس التكلفة والمخزون") { s ->
            s.commerceEngine.postSaleReturn(invoiceId, LocalDate.now().toString(), reason)
        }
    }

    fun returnPurchaseInvoice(invoiceId: String, reason: String) {
        runMutation("تم إنشاء مرتجع المشتريات وتخفيض المخزون والذمم") { s ->
            s.commerceEngine.postPurchaseReturn(invoiceId, LocalDate.now().toString(), reason)
        }
    }

    fun shareInvoicePdf(context: android.content.Context, invoiceId: String) {
        val session = sessionFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inv = session.db.commerceDao().getInvoiceById(invoiceId) ?: return@launch
                val lines = session.db.commerceDao().getLinesForInvoice(invoiceId)
                val profile = session.db.accountingDao().getProfile()
                viewModelScope.launch(Dispatchers.Main) {
                    com.daftar.app.core.print.InvoicePdfPrinter.generateAndShareInvoicePdf(
                        context = context,
                        profile = profile,
                        invoice = inv,
                        lines = lines,
                    )
                }
            } catch (e: Throwable) {
                _snapshot.update { it.copy(errorMessage = e.message ?: "PDF generation failed") }
            }
        }
    }

    fun issuePurchaseInvoice(
        partyId: String?,
        paymentMethod: String,
        currencyCode: String,
        exchangeRate: BigDecimal,
        taxRate: BigDecimal,
        lines: List<InvoiceLineRequest>,
        batchNumber: String? = null,
        expiryDateIso: String? = null,
    ) {
        runMutation("تم ترحيل فاتورة المشتريات وتحديث المخزون") { s ->
            val whId = _snapshot.value.warehouses.firstOrNull()?.id ?: "WH-01"
            s.commerceEngine.postPurchaseInvoice(
                issueDateIso = LocalDate.now().toString(),
                partyId = partyId,
                warehouseId = whId,
                paymentMethod = paymentMethod,
                currencyCode = currencyCode,
                exchangeRateAtTx = exchangeRate,
                lines = lines.map { it.copy(taxRatePercent = taxRate) },
                batchNumber = batchNumber,
                expiryDateIso = expiryDateIso,
            )
        }
    }

    fun createVoucher(voucherType: String, debitAcc: String, creditAcc: String, amount: BigDecimal, desc: String) {
        runMutation("تم ترحيل السند وإنشاء قيده المحاسبي") { s ->
            s.commerceEngine.postVoucher(
                voucherType = voucherType,
                dateIso = LocalDate.now().toString(),
                debitAccountId = debitAcc,
                creditAccountId = creditAcc,
                amountOriginal = amount,
                currencyCode = _snapshot.value.currency,
                description = desc,
            )
        }
    }

    fun reconcileBank(treasuryId: String, statementBal: BigDecimal, notes: String) {
        runMutation("تم حفظ تسوية الحساب النقدي/البنكي") { s ->
            val tr = s.db.commerceDao().getTreasuryById(treasuryId) ?: error("Treasury not found")
            val bookBal = s.doubleEntry.getAccountBalanceBase(tr.linkedGlAccountId)
            val today = LocalDate.now().toString()
            s.db.commerceDao().insertReconciliation(
                BankReconciliationEntity(
                    id = UUID.randomUUID().toString(),
                    treasuryAccountId = treasuryId,
                    statementDateIso = today,
                    statementBalance = statementBal,
                    bookBalanceAtTime = bookBal,
                    difference = statementBal.subtract(bookBal),
                    notes = notes,
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
            s.db.commerceDao().upsertTreasury(tr.copy(lastReconciledDateIso = today, lastStatementBalance = statementBal))
        }
    }

    fun transferStock(itemId: String, fromWh: String, toWh: String, qty: BigDecimal) {
        runMutation("تم تحويل المخزون بين المستودعات") { s ->
            s.inventoryEngine.transferBetweenWarehouses(itemId, fromWh, toWh, qty, LocalDate.now().toString(), "WH-XFER")
        }
    }

    fun adjustStock(itemId: String, warehouseId: String, deltaQty: BigDecimal, reason: String) {
        runMutation("تم تنفيذ التسوية الجردية وترحيل القيد") { s ->
            s.inventoryEngine.recordStockAdjustment(itemId, warehouseId, deltaQty, LocalDate.now().toString(), reason)
        }
    }

    fun addWarehouse(code: String, nameAr: String) {
        runMutation("تم إضافة المستودع") { s ->
            s.db.inventoryDao().upsertWarehouse(
                WarehouseEntity(
                    id = "WH-${System.currentTimeMillis() % 10000}",
                    code = code,
                    nameAr = nameAr,
                    nameEn = nameAr,
                    branchId = _snapshot.value.branches.firstOrNull()?.id ?: "BR-01",
                )
            )
        }
    }

    fun addBranch(code: String, nameAr: String, city: String) {
        runMutation("تم إضافة الفرع") { s ->
            s.db.inventoryDao().upsertBranch(
                BranchEntity(
                    id = "BR-${System.currentTimeMillis() % 10000}",
                    code = code,
                    nameAr = nameAr,
                    nameEn = nameAr,
                    city = city,
                )
            )
        }
    }

    fun addCurrency(code: String, nameAr: String, rateToBase: BigDecimal, decimals: Int) {
        runMutation("تم تحديث سعر صرف $code") { s ->
            s.db.accountingDao().upsertCurrency(
                CurrencyEntity(
                    code = code.uppercase().trim(),
                    nameAr = nameAr,
                    nameEn = code.uppercase().trim(),
                    symbol = code.uppercase().trim(),
                    decimalPlaces = decimals,
                    exchangeRateToBase = rateToBase,
                    isBaseCurrency = code.equals(_snapshot.value.currency, ignoreCase = true),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun addTaxRule(nameAr: String, ratePercent: BigDecimal) {
        runMutation("تم إضافة قاعدة ضريبة") { s ->
            s.db.accountingDao().upsertTaxRule(
                TaxRuleEntity(
                    id = UUID.randomUUID().toString(),
                    nameAr = nameAr,
                    nameEn = nameAr,
                    ratePercent = ratePercent,
                )
            )
        }
    }

    fun addEmployee(name: String, role: String, salary: BigDecimal) {
        runMutation("تم حفظ بيانات الموظف") { s ->
            s.db.operationsDao().upsertEmployee(
                EmployeeEntity(
                    id = UUID.randomUUID().toString(),
                    code = "EMP-${System.currentTimeMillis() % 1000}",
                    fullName = name,
                    jobTitle = role,
                    baseSalaryBase = salary,
                )
            )
        }
    }

    fun runMonthlyPayroll(periodLabel: String) {
        runMutation("تم اعتماد مسير الرواتب وترحيل قيد الاستحقاق/الصرف") { s ->
            s.commerceEngine.postPayrollRun(periodLabel, LocalDate.now().toString())
        }
    }

    fun addFixedAsset(nameAr: String, cost: BigDecimal, salvage: BigDecimal, lifeMonths: Int) {
        runMutation("تم تسجيل الأصل الثابت وإثباته محاسبياً") { s ->
            val asset = FixedAssetEntity(
                id = UUID.randomUUID().toString(),
                assetCode = "FA-${System.currentTimeMillis() % 1000}",
                nameAr = nameAr,
                nameEn = nameAr,
                purchaseDateIso = LocalDate.now().toString(),
                acquisitionCostBase = cost,
                salvageValueBase = salvage,
                usefulLifeMonths = lifeMonths,
            )
            s.db.operationsDao().upsertFixedAsset(asset)
            s.doubleEntry.postJournalEntry(
                transactionDateIso = asset.purchaseDateIso,
                description = "شراء أصل ثابت: $nameAr",
                sourceModule = "FIXED_ASSETS",
                lines = listOf(
                    JournalLineDraft("1201", debitBase = cost),
                    JournalLineDraft("1101", creditBase = cost),
                )
            )
        }
    }

    fun runMonthlyDepreciation(assetId: String) {
        runMutation("تم ترحيل قسط الإهلاك الشهري للأصل") { s ->
            val asset = s.db.operationsDao().getFixedAssetById(assetId) ?: error("Asset not found")
            val depreciable = (asset.acquisitionCostBase.subtract(asset.salvageValueBase)).max(BigDecimal.ZERO)
            val monthly = MoneyMath.divide(depreciable, BigDecimal(asset.usefulLifeMonths.coerceAtLeast(1)), 2)
            require(monthly > BigDecimal.ZERO) { "Asset fully depreciated" }
            s.doubleEntry.postJournalEntry(
                transactionDateIso = LocalDate.now().toString(),
                description = "إهلاك شهري للأصل ${asset.nameAr}",
                sourceModule = "DEPRECIATION",
                lines = listOf(
                    JournalLineDraft(asset.deprExpenseAccountId, debitBase = monthly),
                    JournalLineDraft(asset.accumDeprAccountId, creditBase = monthly),
                )
            )
            s.db.operationsDao().upsertFixedAsset(
                asset.copy(accumulatedDepreciationBase = asset.accumulatedDepreciationBase.add(monthly))
            )
        }
    }

    fun addBudget(accountId: String, planned: BigDecimal) {
        runMutation("تم اعتماد بند الميزانية التقديرية") { s ->
            val periodId = _snapshot.value.fiscalPeriods.firstOrNull()?.id ?: "FY-2026"
            s.db.operationsDao().upsertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    fiscalPeriodId = periodId,
                    accountId = accountId,
                    plannedAmountBase = planned,
                )
            )
        }
    }

    fun addProject(nameAr: String, contractValue: BigDecimal) {
        runMutation("تم إنشاء المشروع ومركز التكلفة المرتبط") { s ->
            val ccId = "CC-${System.currentTimeMillis() % 10000}"
            s.db.accountingDao().upsertCostCenter(
                CostCenterEntity(ccId, ccId, nameAr, nameAr)
            )
            s.db.specializedDao().upsertProject(
                ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    code = "PRJ-${System.currentTimeMillis() % 1000}",
                    nameAr = nameAr,
                    nameEn = nameAr,
                    costCenterId = ccId,
                    contractValueBase = contractValue,
                    startDateIso = LocalDate.now().toString(),
                )
            )
        }
    }

    fun addRestaurantOrder(tableNo: String, summary: String, total: BigDecimal) {
        runMutation("تم إرسال طلب المطبخ وتحديث الطاولة") { s ->
            val tblId = "TBL-$tableNo"
            val orderId = UUID.randomUUID().toString()
            s.db.specializedDao().upsertRestaurantTable(
                RestaurantTableEntity(tblId, tableNo, "الصالة", 4, "OCCUPIED", orderId)
            )
            s.db.specializedDao().upsertKitchenOrder(
                KitchenOrderEntity(orderId, "KOT-${System.currentTimeMillis() % 1000}", tblId, "DINE_IN", summary, total, "PREPARING", createdAtMillis = System.currentTimeMillis())
            )
        }
    }

    fun addWorkshopJob(assetDesc: String, issue: String, partsCost: BigDecimal, laborFee: BigDecimal) {
        runMutation("تم فتح أمر صيانة جديد") { s ->
            s.db.specializedDao().upsertWorkshopJob(
                WorkshopJobEntity(
                    id = UUID.randomUUID().toString(),
                    jobNumber = "JOB-${System.currentTimeMillis() % 1000}",
                    customerId = null,
                    assetDescription = assetDesc,
                    issueSummary = issue,
                    sparePartsCostBase = partsCost,
                    laborFeeBase = laborFee,
                    status = "IN_REPAIR",
                    createdDateIso = LocalDate.now().toString(),
                )
            )
        }
    }

    fun createProductionOrder(itemId: String, qty: BigDecimal, unitCost: BigDecimal) {
        runMutation("تم تنفيذ أمر الإنتاج وترحيل التكلفة") { s ->
            val totalCost = MoneyMath.multiply(qty, unitCost, 2)
            val whId = _snapshot.value.warehouses.firstOrNull()?.id ?: "WH-01"
            val je = s.doubleEntry.postJournalEntry(
                transactionDateIso = LocalDate.now().toString(),
                description = "إثبات إنتاج تام الصنع",
                sourceModule = "MANUFACTURING",
                lines = listOf(
                    JournalLineDraft("1104", debitBase = totalCost),
                    JournalLineDraft("5101", creditBase = totalCost),
                )
            )
            s.inventoryEngine.receiveStock(itemId, whId, qty, unitCost, "PRD-${System.currentTimeMillis() % 1000}", je.id)
            s.db.specializedDao().upsertProductionOrder(
                ProductionOrderEntity(
                    id = UUID.randomUUID().toString(),
                    orderNumber = "PRD-${System.currentTimeMillis() % 1000}",
                    bomId = "DIRECT",
                    finishedItemId = itemId,
                    quantityPlanned = qty,
                    quantityProduced = qty,
                    warehouseId = whId,
                    totalCostBase = totalCost,
                    status = "COMPLETED",
                    journalEntryId = je.id,
                    dateIso = LocalDate.now().toString(),
                )
            )
        }
    }

    fun addShippingOrder(recipient: String, city: String, carrier: String, codAmount: BigDecimal, fee: BigDecimal) {
        runMutation("تم إنشاء بوليصة الشحن") { s ->
            s.db.specializedDao().upsertShippingOrder(
                ShippingOrderEntity(
                    id = UUID.randomUUID().toString(),
                    waybillNumber = "WB-${System.currentTimeMillis() % 100000}",
                    invoiceId = null,
                    recipientName = recipient,
                    destinationCity = city,
                    carrierName = carrier,
                    codAmountBase = codAmount,
                    shippingFeeBase = fee,
                    status = "IN_TRANSIT",
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun addLoyaltyOffer(nameAr: String, discountPercent: BigDecimal) {
        runMutation("تم تفعيل العرض الترويجي") { s ->
            s.db.specializedDao().upsertLoyaltyOffer(
                LoyaltyOfferEntity(UUID.randomUUID().toString(), nameAr, nameAr, discountPercent, 1)
            )
        }
    }

    fun addUser(fullName: String, username: String, role: UserRole, pin: String) {
        runMutation("تم إنشاء حساب المستخدم وتشفير رمز PIN") { s ->
            val hashed = PinHasher.hashPin(pin)
            s.db.governanceDao().insertUser(
                UserEntity(
                    id = UUID.randomUUID().toString(),
                    fullName = fullName,
                    username = username,
                    roleId = role.id,
                    permissionsCsv = RbacPermission.serialize(RbacPermission.defaultsForRole(role)),
                    pinHash = hashed.hashBase64,
                    pinSalt = hashed.saltBase64,
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun addAttachmentRecord(title: String, linkedType: String, linkedId: String) {
        runMutation("تمت أرشفة المستند محلياً") { s ->
            s.db.operationsDao().insertAttachment(
                AttachmentEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    fileName = "$title.pdf",
                    mimeType = "application/pdf",
                    sha256Hex = UUID.randomUUID().toString().replace("-", ""),
                    sizeBytes = 24_576L,
                    linkedEntityType = linkedType,
                    linkedEntityId = linkedId,
                    storedRelativePath = "attachments/$title.pdf",
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun updateCostingMethod(methodId: String) {
        runMutation("تم تحديث سياسة تقييم المخزون") { s ->
            val profile = s.db.accountingDao().getProfile() ?: return@runMutation
            s.db.accountingDao().upsertProfile(profile.copy(costingMethodId = methodId, updatedAt = System.currentTimeMillis()))
        }
    }

    fun askLocalAssistant(query: String, language: AppLanguage) {
        val session = sessionFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val reply = session.assistantEngine.answerQuery(query, language)
            _snapshot.update { it.copy(assistantHistory = listOf(reply) + it.assistantHistory) }
        }
    }

    fun markAllNotificationsRead() {
        val session = sessionFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            session.db.governanceDao().markAllNotificationsRead()
            refreshAll(session, FinancialStatementsEngine(session.db))
        }
    }
}
