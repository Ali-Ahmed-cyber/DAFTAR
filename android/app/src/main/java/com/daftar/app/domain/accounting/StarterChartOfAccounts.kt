package com.daftar.app.domain.accounting

import androidx.room.withTransaction
import com.daftar.app.core.model.BusinessType
import com.daftar.app.core.model.DaftarModulesRegistry
import com.daftar.app.core.security.PinHasher
import com.daftar.app.core.security.RbacPermission
import com.daftar.app.core.security.UserRole
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

object StarterChartOfAccounts {

    suspend fun initializeNewActivityDatabase(
        db: ActivityDatabase,
        vaultId: String,
        nameAr: String,
        nameEn: String,
        businessType: BusinessType,
        countryCode: String = "SD",
        baseCurrency: String = "SDG",
        decimalPlaces: Int = 2,
        fiscalYearStart: String = "2026-01-01",
        costingMethodId: String = "wac",
        adminFullName: String,
        adminPin: String,
    ): UserEntity {
        val now = System.currentTimeMillis()
        val pinHash = PinHasher.hashPin(adminPin)
        val ownerUser = UserEntity(
            id = UUID.randomUUID().toString(),
            fullName = adminFullName.ifBlank { "المالك" },
            username = "owner",
            roleId = UserRole.OWNER.id,
            permissionsCsv = RbacPermission.serialize(RbacPermission.defaultsForRole(UserRole.OWNER)),
            pinHash = pinHash.hashBase64,
            pinSalt = pinHash.saltBase64,
            biometricEnabled = true,
            isActive = true,
            createdAtMillis = now,
        )

        val enabledModules = DaftarModulesRegistry.modulesForBusiness(businessType)
            .joinToString(",") { it.id }

        val fyYear = fiscalYearStart.take(4).ifBlank { LocalDate.now().year.toString() }
        val fyEnd = "${fyYear}-12-31"

        db.withTransaction {
            val accDao = db.accountingDao()
            val comDao = db.commerceDao()
            val invDao = db.inventoryDao()
            val govDao = db.governanceDao()
            val specDao = db.specializedDao()

            accDao.upsertProfile(
                ActivityProfileEntity(
                    id = vaultId,
                    nameAr = nameAr,
                    nameEn = nameEn,
                    businessTypeId = businessType.id,
                    countryCode = countryCode,
                    baseCurrency = baseCurrency,
                    decimalPlaces = decimalPlaces,
                    fiscalYearStart = fiscalYearStart,
                    costingMethodId = costingMethodId,
                    enabledModulesCsv = enabledModules,
                    logoPrintCaption = "DAFTAR | $nameAr",
                    updatedAt = now,
                )
            )

            // Default Sudan currency (SDG) + optional FX currencies (non-forced)
            accDao.upsertCurrency(
                CurrencyEntity(
                    code = baseCurrency,
                    nameAr = if (baseCurrency == "SDG") "الجنيه السوداني" else baseCurrency,
                    nameEn = if (baseCurrency == "SDG") "Sudanese Pound" else baseCurrency,
                    symbol = baseCurrency,
                    decimalPlaces = decimalPlaces,
                    exchangeRateToBase = BigDecimal.ONE,
                    isBaseCurrency = true,
                    updatedAt = now,
                )
            )
            if (baseCurrency != "USD") {
                accDao.upsertCurrency(
                    CurrencyEntity(
                        code = "USD",
                        nameAr = "الدولار الأمريكي",
                        nameEn = "US Dollar",
                        symbol = "USD",
                        decimalPlaces = 2,
                        exchangeRateToBase = BigDecimal("2500.00"),
                        isBaseCurrency = false,
                        updatedAt = now,
                    )
                )
            }
            if (baseCurrency != "SAR") {
                accDao.upsertCurrency(
                    CurrencyEntity(
                        code = "SAR",
                        nameAr = "الريال السعودي",
                        nameEn = "Saudi Riyal",
                        symbol = "SAR",
                        decimalPlaces = 2,
                        exchangeRateToBase = BigDecimal("666.00"),
                        isBaseCurrency = false,
                        updatedAt = now,
                    )
                )
            }

            // Configurable tax rules (user editable, default VAT 0% or 17% toggleable)
            accDao.upsertTaxRule(
                TaxRuleEntity(
                    id = "tax-exempt",
                    nameAr = "معفى من الضريبة (0%)",
                    nameEn = "Tax Exempt (0%)",
                    ratePercent = BigDecimal.ZERO,
                    isDefault = true,
                )
            )
            accDao.upsertTaxRule(
                TaxRuleEntity(
                    id = "tax-vat-std",
                    nameAr = "ضريبة القيمة المضافة القياسية (17%)",
                    nameEn = "Standard VAT (17%)",
                    ratePercent = BigDecimal("17.00"),
                    isDefault = false,
                )
            )

            accDao.upsertFiscalPeriod(
                FiscalPeriodEntity(
                    id = "FY-$fyYear",
                    nameAr = "السنة المالية $fyYear",
                    nameEn = "Fiscal Year $fyYear",
                    startDateIso = fiscalYearStart,
                    endDateIso = fyEnd,
                    isClosed = false,
                )
            )

            accDao.upsertCostCenter(
                CostCenterEntity(
                    id = "CC-MAIN",
                    code = "CC-01",
                    nameAr = "المركز الرئيسي",
                    nameEn = "Headquarters",
                    isActive = true,
                )
            )

            accDao.insertAccounts(buildAccounts(baseCurrency))

            comDao.upsertTreasury(
                TreasuryAccountEntity(
                    id = "TR-CASH-MAIN",
                    nameAr = "الخزينة الرئيسية",
                    nameEn = "Main Cash Box",
                    kind = "CASH_BOX",
                    linkedGlAccountId = "1101",
                    currencyCode = baseCurrency,
                    isActive = true,
                )
            )
            comDao.upsertTreasury(
                TreasuryAccountEntity(
                    id = "TR-BANK-MAIN",
                    nameAr = "الحساب البنكي الرئيسي",
                    nameEn = "Primary Bank Account",
                    kind = "BANK",
                    linkedGlAccountId = "1102",
                    currencyCode = baseCurrency,
                    bankAccountNumber = "SD-BANK-001",
                    isActive = true,
                )
            )

            invDao.upsertBranch(
                BranchEntity(
                    id = "BR-01",
                    code = "BR-01",
                    nameAr = "الفرع الرئيسي",
                    nameEn = "Main Branch",
                    city = if (countryCode == "SD") "الخرطوم" else "Main City",
                    costCenterId = "CC-MAIN",
                    isPrimary = true,
                )
            )
            invDao.upsertWarehouse(
                WarehouseEntity(
                    id = "WH-01",
                    code = "WH-01",
                    nameAr = "المستودع الرئيسي",
                    nameEn = "Main Warehouse",
                    branchId = "BR-01",
                    isPrimary = true,
                )
            )

            specDao.upsertPartnerStake(
                PartnerStakeEntity(
                    id = "PTN-01",
                    partnerName = adminFullName.ifBlank { "الشريك المؤسس" },
                    sharePercent = BigDecimal("100.00"),
                    contributedCapitalBase = BigDecimal.ZERO,
                )
            )

            govDao.insertUser(ownerUser)
            govDao.appendAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    userId = ownerUser.id,
                    userName = ownerUser.fullName,
                    actionType = "ACTIVITY_INITIALIZED",
                    entityType = "ACTIVITY",
                    entityId = vaultId,
                    summary = "Initialized $businessType vault in $baseCurrency with double-entry CoA",
                    isSecurityEvent = true,
                )
            )
        }

        return ownerUser
    }

    private fun buildAccounts(currency: String): List<AccountEntity> = listOf(
        // 1 Assets
        AccountEntity("1000", "1000", "الأصول", "Assets", "ASSET", "DEBIT", null, isGroup = true, currencyCode = currency),
        AccountEntity("1100", "1100", "الأصول المتداولة", "Current Assets", "ASSET", "DEBIT", "1000", isGroup = true, currencyCode = currency),
        AccountEntity("1101", "1101", "الخزينة النقدية", "Cash on Hand", "ASSET", "DEBIT", "1100", isGroup = false, currencyCode = currency),
        AccountEntity("1102", "1102", "البنك والحسابات الجارية", "Bank Accounts", "ASSET", "DEBIT", "1100", isGroup = false, currencyCode = currency),
        AccountEntity("1103", "1103", "العملاء والذمم المدينة", "Accounts Receivable", "ASSET", "DEBIT", "1100", isGroup = false, currencyCode = currency),
        AccountEntity("1104", "1104", "المخزون السلعي", "Inventory Asset", "ASSET", "DEBIT", "1100", isGroup = false, currencyCode = currency),
        AccountEntity("1105", "1105", "ضريبة مدخلات مستردة", "Input VAT Receivable", "ASSET", "DEBIT", "1100", isGroup = false, currencyCode = currency),
        AccountEntity("1106", "1106", "سلف وعهد الموظفين", "Employee Advances", "ASSET", "DEBIT", "1100", isGroup = false, currencyCode = currency),
        AccountEntity("1200", "1200", "الأصول الثابتة", "Fixed Assets", "ASSET", "DEBIT", "1000", isGroup = true, currencyCode = currency),
        AccountEntity("1201", "1201", "المعدات والأصول الثابتة", "Property & Equipment", "ASSET", "DEBIT", "1200", isGroup = false, currencyCode = currency),
        AccountEntity("1209", "1209", "مجمع الإهلاك", "Accumulated Depreciation", "ASSET", "CREDIT", "1200", isGroup = false, currencyCode = currency),

        // 2 Liabilities
        AccountEntity("2000", "2000", "الالتزامات", "Liabilities", "LIABILITY", "CREDIT", null, isGroup = true, currencyCode = currency),
        AccountEntity("2100", "2100", "الالتزامات المتداولة", "Current Liabilities", "LIABILITY", "CREDIT", "2000", isGroup = true, currencyCode = currency),
        AccountEntity("2101", "2101", "الموردون والذمم الدائنة", "Accounts Payable", "LIABILITY", "CREDIT", "2100", isGroup = false, currencyCode = currency),
        AccountEntity("2102", "2102", "الضرائب والرسوم المستحقة", "Output VAT & Taxes Payable", "LIABILITY", "CREDIT", "2100", isGroup = false, currencyCode = currency),
        AccountEntity("2103", "2103", "رواتب وأجور مستحقة", "Salaries Payable", "LIABILITY", "CREDIT", "2100", isGroup = false, currencyCode = currency),

        // 3 Equity
        AccountEntity("3000", "3000", "حقوق الملكية", "Equity", "EQUITY", "CREDIT", null, isGroup = true, currencyCode = currency),
        AccountEntity("3101", "3101", "رأس المال", "Share Capital", "EQUITY", "CREDIT", "3000", isGroup = false, currencyCode = currency),
        AccountEntity("3102", "3102", "جاري الشركاء والمسحوبات", "Partner Drawings & Current", "EQUITY", "DEBIT", "3000", isGroup = false, currencyCode = currency),
        AccountEntity("3103", "3103", "الأرباح المبقاة", "Retained Earnings", "EQUITY", "CREDIT", "3000", isGroup = false, currencyCode = currency),

        // 4 Revenues
        AccountEntity("4000", "4000", "الإيرادات", "Revenues", "REVENUE", "CREDIT", null, isGroup = true, currencyCode = currency),
        AccountEntity("4101", "4101", "إيرادات المبيعات والخدمات", "Sales & Service Revenue", "REVENUE", "CREDIT", "4000", isGroup = false, currencyCode = currency),
        AccountEntity("4102", "4102", "مردودات ومسموحات المبيعات", "Sales Returns & Allowances", "REVENUE", "DEBIT", "4000", isGroup = false, currencyCode = currency),
        AccountEntity("4103", "4103", "خصم مسموح به", "Sales Discounts", "REVENUE", "DEBIT", "4000", isGroup = false, currencyCode = currency),
        AccountEntity("4201", "4201", "إيرادات أخرى", "Other Operating Income", "REVENUE", "CREDIT", "4000", isGroup = false, currencyCode = currency),

        // 5 Expenses
        AccountEntity("5000", "5000", "المصروفات والتكاليف", "Expenses & COGS", "EXPENSE", "DEBIT", null, isGroup = true, currencyCode = currency),
        AccountEntity("5101", "5101", "تكلفة البضاعة المباعة", "Cost of Goods Sold (COGS)", "EXPENSE", "DEBIT", "5000", isGroup = false, currencyCode = currency),
        AccountEntity("5102", "5102", "تعديلات وفروقات المخزون", "Inventory Adjustment Expense", "EXPENSE", "DEBIT", "5000", isGroup = false, currencyCode = currency),
        AccountEntity("5201", "5201", "مصروفات الرواتب والأجور", "Payroll & Salaries Expense", "EXPENSE", "DEBIT", "5000", isGroup = false, currencyCode = currency),
        AccountEntity("5202", "5202", "مصروفات الإيجار والمرافق", "Rent & Utilities Expense", "EXPENSE", "DEBIT", "5000", isGroup = false, currencyCode = currency),
        AccountEntity("5203", "5203", "مصروفات تشغيلية وإدارية", "Operating & Admin Expense", "EXPENSE", "DEBIT", "5000", isGroup = false, currencyCode = currency),
        AccountEntity("5204", "5204", "خصم مكتسب (تخفيض مشتريات)", "Purchase Discounts Earned", "EXPENSE", "CREDIT", "5000", isGroup = false, currencyCode = currency),
        AccountEntity("5205", "5205", "مصروف الإهلاك", "Depreciation Expense", "EXPENSE", "DEBIT", "5000", isGroup = false, currencyCode = currency),
    )
}
