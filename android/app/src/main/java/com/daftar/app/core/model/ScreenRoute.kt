package com.daftar.app.core.model

enum class ScreenGroup(val title: BilingualText) {
    ONBOARDING(BilingualText("التهيئة والدخول", "Onboarding & Auth")),
    WORKSPACE(BilingualText("القيادة والتنبيهات", "Command & Alerts")),
    ACCOUNTING(BilingualText("المحاسبة والقوائم", "Accounting & Statements")),
    COMMERCE(BilingualText("التجارة والمخزون", "Commerce & Ops")),
    SPECIALIZED(BilingualText("الأنشطة المتخصصة", "Specialized Sectors")),
    GOVERNANCE(BilingualText("التقارير والحماية والذكاء", "Audit, Vault & AI")),
}

enum class ScreenRoute(
    val route: String,
    val order: Int,
    val group: ScreenGroup,
    val title: BilingualText,
    val subtitle: BilingualText,
    val showBottomNav: Boolean = true,
) {
    SPLASH("splash", 1, ScreenGroup.ONBOARDING, BilingualText("دفتر", "DAFTAR"), BilingualText("فحص الحافظات المشفرة", "Vault integrity check"), false),
    BUSINESS_TYPE("business-type", 2, ScreenGroup.ONBOARDING, BilingualText("اختيار نوع النشاط", "Business Type"), BilingualText("تفعيل الوحدات القطاعية", "Sector module setup"), false),
    CREATE_ACTIVITY("create-activity", 3, ScreenGroup.ONBOARDING, BilingualText("إنشاء نشاط مستقل", "Create Activity"), BilingualText("قاعدة بيانات وعملة مستقلة", "Isolated DB & currency"), false),
    LOGIN("login", 4, ScreenGroup.ONBOARDING, BilingualText("تسجيل الدخول", "Activity Login"), BilingualText("PIN وبصمة الإصبع", "PIN & Biometric unlock"), false),
    RESTORE("restore", 5, ScreenGroup.ONBOARDING, BilingualText("استعادة البيانات", "Encrypted Restore"), BilingualText("فك التشفير وفحص السلامة", "Decrypt & verify integrity"), false),

    DASHBOARD("dashboard", 6, ScreenGroup.WORKSPACE, BilingualText("لوحة التحكم", "Dashboard"), BilingualText("المؤشرات والسيولة", "KPIs & liquidity")),
    MAIN_MENU("main-menu", 7, ScreenGroup.WORKSPACE, BilingualText("القائمة الرئيسية", "Main Menu"), BilingualText("الوحدات المفعلة للنشاط", "Active sector modules")),
    NOTIFICATIONS("notifications", 8, ScreenGroup.WORKSPACE, BilingualText("الإشعارات المحلية", "Notifications"), BilingualText("تنبيهات WorkManager", "Local alerts")),

    ACCOUNTS("accounts", 9, ScreenGroup.ACCOUNTING, BilingualText("دليل الحسابات", "Chart of Accounts"), BilingualText("الشجرة المحاسبية ومراكز التكلفة", "CoA & cost centers")),
    JOURNAL_ENTRIES("journal-entries", 10, ScreenGroup.ACCOUNTING, BilingualText("القيود اليومية", "Journal Entries"), BilingualText("تحقق المدين = الدائن والعكسي", "Balanced & reversals")),
    GENERAL_LEDGER("general-ledger", 11, ScreenGroup.ACCOUNTING, BilingualText("دفتر الأستاذ العام", "General Ledger"), BilingualText("حركة الحساب والرصيد التراكمي", "Movements & running balance")),
    TREASURY_BANKS("treasury-banks", 12, ScreenGroup.ACCOUNTING, BilingualText("الخزائن والبنوك", "Treasury & Banks"), BilingualText("الصناديق والتحويلات والتسوية", "Vaults & reconciliation")),
    EXPENSES_REVENUES("expenses-revenues", 13, ScreenGroup.ACCOUNTING, BilingualText("المصروفات والإيرادات", "Expenses & Revenues"), BilingualText("سندات الصرف والقبض", "Vouchers & cost centers")),
    FIXED_ASSETS("fixed-assets", 14, ScreenGroup.ACCOUNTING, BilingualText("الأصول الثابتة والإهلاك", "Fixed Assets"), BilingualText("القيمة الدفترية والإهلاك", "Assets & depreciation")),
    TRIAL_BALANCE("trial-balance", 15, ScreenGroup.ACCOUNTING, BilingualText("ميزان المراجعة", "Trial Balance"), BilingualText("تطابق المجاميع والأرصدة", "Debit & credit parity")),
    FINANCIAL_STATEMENTS("financial-statements", 16, ScreenGroup.ACCOUNTING, BilingualText("القوائم المالية والإقفال", "Financial Statements"), BilingualText("الدخل والميزانية وإقفال الفترة", "P&L, Balance Sheet & Closing")),
    CASH_FLOWS("cash-flows", 17, ScreenGroup.ACCOUNTING, BilingualText("التدفقات النقدية", "Cash Flows"), BilingualText("التشغيلي والاستثماري والتمويلي", "Operating/Investing/Financing")),

    SALES("sales", 18, ScreenGroup.COMMERCE, BilingualText("المبيعات والفواتير", "Sales & Invoices"), BilingualText("الفواتير والمرتجعات", "Invoices & returns")),
    CREATE_INVOICE("create-invoice", 19, ScreenGroup.COMMERCE, BilingualText("إنشاء فاتورة", "Invoice Builder"), BilingualText("الضريبة وسعر الصرف والقيد", "Tax, FX lock & preview")),
    POS_TERMINAL("pos-terminal", 20, ScreenGroup.COMMERCE, BilingualText("نقطة البيع POS", "Point of Sale"), BilingualText("كاشير سريع وباركود", "Fast cashier & barcode")),
    PURCHASES("purchases", 21, ScreenGroup.COMMERCE, BilingualText("المشتريات", "Purchases"), BilingualText("فواتير الشراء والمرتجعات", "Supplier bills & returns")),
    PARTIES("parties", 22, ScreenGroup.COMMERCE, BilingualText("العملاء والموردون", "Customers & Suppliers"), BilingualText("الذمم وكشوف الحساب", "CRM, AP & statements")),
    INVENTORY("inventory", 23, ScreenGroup.COMMERCE, BilingualText("المخزون والمستودعات", "Inventory & Warehouses"), BilingualText("تحويل وجرد وتقييم", "Stocktake, transfer & FIFO/WAC")),
    BARCODE_QR("barcode-qr", 24, ScreenGroup.COMMERCE, BilingualText("الباركود وQR", "Barcode & QR"), BilingualText("ملصقات الأصناف والفواتير", "Labels & offline scanner")),
    EMPLOYEES("employees", 25, ScreenGroup.COMMERCE, BilingualText("الموظفون والرواتب", "Employees & Payroll"), BilingualText("مسير الرواتب والسلف", "Staff, advances & posting")),
    BUDGETS("budgets", 26, ScreenGroup.COMMERCE, BilingualText("الميزانيات والتخطيط", "Budgets & Planning"), BilingualText("المخطط مقابل الفعلي", "Planned vs actual variance")),
    ATTACHMENTS("attachments", 27, ScreenGroup.COMMERCE, BilingualText("المرفقات والمستندات", "Attachments & Docs"), BilingualText("أرشفة مشفرة مرتبطة بالقيود", "Encrypted local archive")),

    BRANCHES("branches", 28, ScreenGroup.SPECIALIZED, BilingualText("الفروع والمواقع", "Branches & Locations"), BilingualText("فروع النشاط ومراكزها", "Multi-branch performance")),
    PROJECTS("projects", 29, ScreenGroup.SPECIALIZED, BilingualText("المشاريع ومراكز التكلفة", "Projects & Cost Centers"), BilingualText("العقود والمستخلصات والربحية", "Contracts & margins")),
    MANUFACTURING("manufacturing", 30, ScreenGroup.SPECIALIZED, BilingualText("المصانع والإنتاج", "Manufacturing & BOM"), BilingualText("المواد الخام وأوامر الإنتاج", "BOM & unit cost")),
    RESTAURANTS("restaurants", 31, ScreenGroup.SPECIALIZED, BilingualText("المطاعم والطلبات", "Restaurants & Kitchen"), BilingualText("الطاولات وتذاكر المطبخ", "Floor tables & kitchen")),
    PHARMACIES("pharmacies", 32, ScreenGroup.SPECIALIZED, BilingualText("الصيدليات والصلاحية", "Pharmacies & Batches"), BilingualText("التشغيلات وتواريخ الانتهاء", "FEFO batches & expiry")),
    WORKSHOPS("workshops", 33, ScreenGroup.SPECIALIZED, BilingualText("الورش والصيانة", "Workshops & Repair"), BilingualText("أوامر الصيانة وقطع الغيار", "Job cards & spare parts")),
    SHIPPING("shipping", 34, ScreenGroup.SPECIALIZED, BilingualText("الشحن والتوصيل", "Shipping & Delivery"), BilingualText("البوالص والتحصيل COD", "Waybills & COD settlement")),
    LOYALTY("loyalty", 35, ScreenGroup.SPECIALIZED, BilingualText("العروض والولاء", "Offers & Loyalty"), BilingualText("نقاط العملاء والخصومات", "Points & campaigns")),
    TAXES_CURRENCIES("taxes-currencies", 36, ScreenGroup.SPECIALIZED, BilingualText("الضرائب والعملات", "Taxes & Currencies"), BilingualText("نسب الضرائب وأسعار الصرف", "Configurable tax & FX")),

    REPORTS("reports", 37, ScreenGroup.GOVERNANCE, BilingualText("التقارير ومركز المراجعة", "Reports & Audit Center"), BilingualText("فحص الأخطاء والمؤشرات", "10-point diagnostic audit")),
    AI_ASSISTANT("ai-assistant", 38, ScreenGroup.GOVERNANCE, BilingualText("المساعد الذكي المحلي", "Local Smart Assistant"), BilingualText("تحليل مالي محلي للقراءة فقط", "Offline read-only analyst")),
    BACKUP("backup", 39, ScreenGroup.GOVERNANCE, BilingualText("النسخ الاحتياطي", "Encrypted Backup"), BilingualText("نسخ SAF وفحص الاستعادة", "AES-256 SAF & dry-run")),
    SECURITY("security", 40, ScreenGroup.GOVERNANCE, BilingualText("الحماية وسجل التدقيق", "Security & Audit Log"), BilingualText("SQLCipher ومفتاح الاسترداد", "Keystore & immutable log")),
    SETTINGS("settings", 41, ScreenGroup.GOVERNANCE, BilingualText("الإعدادات والمستخدمون", "Settings & Users"), BilingualText("النشاط، الصلاحيات، اللغة", "Activity, RBAC & theme"));
}
