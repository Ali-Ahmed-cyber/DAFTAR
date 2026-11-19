package com.daftar.app.core.model

enum class ModuleLayer { CORE, OPERATIONS, SPECIALIZED, GOVERNANCE }

data class DaftarModule(
    val number: Int,
    val id: String,
    val title: BilingualText,
    val layer: ModuleLayer,
    val alwaysEnabled: Boolean,
    val businessTypes: Set<BusinessType> = emptySet(),
    val targetRoute: ScreenRoute,
)

object DaftarModulesRegistry {
    val ALL_37_MODULES: List<DaftarModule> = listOf(
        DaftarModule(1, "dashboard", BilingualText("لوحة التحكم", "Dashboard"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.DASHBOARD),
        DaftarModule(2, "activity-settings", BilingualText("إعدادات النشاط", "Activity Settings"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.SETTINGS),
        DaftarModule(3, "chart-of-accounts", BilingualText("دليل الحسابات", "Chart of Accounts"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.ACCOUNTS),
        DaftarModule(4, "journal-entries", BilingualText("القيود اليومية", "Journal Entries"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.JOURNAL_ENTRIES),
        DaftarModule(5, "general-ledger", BilingualText("الأستاذ العام", "General Ledger"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.GENERAL_LEDGER),
        DaftarModule(6, "trial-balance-statements", BilingualText("ميزان المراجعة والقوائم", "Trial Balance & Statements"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.TRIAL_BALANCE),
        DaftarModule(7, "expenses-income", BilingualText("المصروفات والدخل", "Expenses & Income"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.EXPENSES_REVENUES),
        DaftarModule(8, "treasury-banks", BilingualText("الخزائن والحسابات البنكية", "Treasury & Banks"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.TREASURY_BANKS),
        DaftarModule(9, "cash-bank-reconciliation", BilingualText("التسويات النقدية والبنكية", "Cash & Bank Reconciliation"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.TREASURY_BANKS),
        DaftarModule(10, "sales-invoices", BilingualText("المبيعات والفواتير", "Sales & Invoices"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.SALES),
        DaftarModule(11, "purchases", BilingualText("المشتريات", "Purchases"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.PURCHASES),
        DaftarModule(12, "customers-crm", BilingualText("العملاء وإدارة العلاقات", "Customers & CRM"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.PARTIES),
        DaftarModule(13, "suppliers", BilingualText("الموردون", "Suppliers"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.PARTIES),
        DaftarModule(14, "inventory-warehouses", BilingualText("المخزون والمستودعات", "Inventory & Warehouses"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.INVENTORY),
        DaftarModule(15, "products-items", BilingualText("المنتجات والأصناف", "Products & Items"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.INVENTORY),
        DaftarModule(16, "barcode-qr", BilingualText("الباركود وQR", "Barcode & QR"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.RETAIL, BusinessType.RESTAURANT, BusinessType.PHARMACY, BusinessType.WORKSHOP, BusinessType.FACTORY), ScreenRoute.BARCODE_QR),
        DaftarModule(17, "pos", BilingualText("نقاط البيع POS", "Point of Sale"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.RETAIL, BusinessType.RESTAURANT, BusinessType.PHARMACY), ScreenRoute.POS_TERMINAL),
        DaftarModule(18, "employees-payroll", BilingualText("الموظفون والرواتب", "Employees & Payroll"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.EMPLOYEES),
        DaftarModule(19, "fixed-assets", BilingualText("الأصول الثابتة والإهلاك", "Fixed Assets & Depreciation"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.FIXED_ASSETS),
        DaftarModule(20, "budgets-planning", BilingualText("الميزانيات والتخطيط المالي", "Budgets & Financial Planning"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.BUDGETS),
        DaftarModule(21, "taxes-fees", BilingualText("الضرائب والرسوم", "Taxes & Fees"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.TAXES_CURRENCIES),
        DaftarModule(22, "multi-currency", BilingualText("تعدد العملات وأسعار الصرف", "Multi-Currency & FX"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.TAXES_CURRENCIES),
        DaftarModule(23, "branches-locations", BilingualText("الفروع والمواقع", "Branches & Locations"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.RETAIL, BusinessType.FACTORY, BusinessType.SERVICES), ScreenRoute.BRANCHES),
        DaftarModule(24, "projects-cost-centers", BilingualText("المشاريع ومراكز التكلفة", "Projects & Cost Centers"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.WORKSHOP, BusinessType.FACTORY, BusinessType.SERVICES), ScreenRoute.PROJECTS),
        DaftarModule(25, "partners-capital", BilingualText("الشركاء ورأس المال", "Partners & Capital"), ModuleLayer.CORE, true, targetRoute = ScreenRoute.FINANCIAL_STATEMENTS),
        DaftarModule(26, "restaurants-orders", BilingualText("إدارة المطاعم والطلبات", "Restaurants & Orders"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.RESTAURANT), ScreenRoute.RESTAURANTS),
        DaftarModule(27, "pharmacies-batches", BilingualText("الصيدليات والتشغيلات والصلاحية", "Pharmacies, Batches & Expiry"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.PHARMACY), ScreenRoute.PHARMACIES),
        DaftarModule(28, "workshops-maintenance", BilingualText("الورش والصيانة", "Workshops & Maintenance"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.WORKSHOP), ScreenRoute.WORKSHOPS),
        DaftarModule(29, "manufacturing-production", BilingualText("المصانع والإنتاج", "Manufacturing & Production"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.FACTORY, BusinessType.RESTAURANT), ScreenRoute.MANUFACTURING),
        DaftarModule(30, "offers-loyalty", BilingualText("العروض وبرامج الولاء", "Offers & Loyalty"), ModuleLayer.SPECIALIZED, false, setOf(BusinessType.RETAIL, BusinessType.RESTAURANT), ScreenRoute.LOYALTY),
        DaftarModule(31, "reports-analytics", BilingualText("التقارير والإحصائيات", "Reports & Analytics"), ModuleLayer.GOVERNANCE, true, targetRoute = ScreenRoute.REPORTS),
        DaftarModule(32, "documents-attachments", BilingualText("المستندات والمرفقات", "Documents & Attachments"), ModuleLayer.OPERATIONS, true, targetRoute = ScreenRoute.ATTACHMENTS),
        DaftarModule(33, "local-notifications", BilingualText("الإشعارات المحلية", "Local Notifications"), ModuleLayer.GOVERNANCE, true, targetRoute = ScreenRoute.NOTIFICATIONS),
        DaftarModule(34, "smart-assistant", BilingualText("المساعد المحاسبي الذكي", "Local Smart Assistant"), ModuleLayer.GOVERNANCE, true, targetRoute = ScreenRoute.AI_ASSISTANT),
        DaftarModule(35, "backup-restore", BilingualText("النسخ الاحتياطي والاستعادة", "Backup & Restore"), ModuleLayer.GOVERNANCE, true, targetRoute = ScreenRoute.BACKUP),
        DaftarModule(36, "audit-security", BilingualText("سجل التدقيق والحماية", "Audit Log & Security"), ModuleLayer.GOVERNANCE, true, targetRoute = ScreenRoute.SECURITY),
        DaftarModule(37, "users-rbac", BilingualText("إدارة المستخدمين والصلاحيات", "Users & RBAC"), ModuleLayer.GOVERNANCE, true, targetRoute = ScreenRoute.SETTINGS),
    )

    fun modulesForBusiness(type: BusinessType, customEnabledIds: Set<String>? = null): List<DaftarModule> {
        return ALL_37_MODULES.filter { mod ->
            when {
                customEnabledIds != null -> mod.alwaysEnabled || customEnabledIds.contains(mod.id)
                mod.alwaysEnabled -> true
                else -> mod.businessTypes.contains(type)
            }
        }
    }
}
