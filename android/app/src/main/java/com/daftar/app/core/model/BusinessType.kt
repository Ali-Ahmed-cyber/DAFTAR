package com.daftar.app.core.model

enum class BusinessType(
    val id: String,
    val title: BilingualText,
    val summary: BilingualText,
    val defaultCurrency: String = "SDG",
    val defaultCountryCode: String = "SD",
    val specializedModuleNumbers: Set<Int>,
) {
    RETAIL(
        id = "retail",
        title = BilingualText("تجارة وتجزئة", "Retail & Wholesale"),
        summary = BilingualText(
            "نقاط بيع سريعة، باركود، مستودعات متعددة، فروع، ولاء العملاء",
            "Fast POS, barcode labels, multi-warehouse, branches, loyalty"
        ),
        defaultCurrency = "SDG",
        defaultCountryCode = "SD",
        specializedModuleNumbers = setOf(16, 17, 23, 29, 30),
    ),
    RESTAURANT(
        id = "restaurant",
        title = BilingualText("مطاعم ومقاهي", "Restaurants & Cafés"),
        summary = BilingualText(
            "إدارة الطاولات والطلبات، المطبخ، وصفات المواد الخام، التوصيل",
            "Tables & order routing, kitchen queue, recipe BOM, delivery"
        ),
        defaultCurrency = "SDG",
        defaultCountryCode = "SD",
        specializedModuleNumbers = setOf(16, 17, 25, 26, 29, 30),
    ),
    PHARMACY(
        id = "pharmacy",
        title = BilingualText("صيدليات ومستلزمات طبية", "Pharmacies & Medical"),
        summary = BilingualText(
            "تتبع التشغيلات، تواريخ الصلاحية، الباركود الدوائي، التنبيهات المبكرة",
            "Batch tracking, FEFO expiry alerts, medical barcode, compliance"
        ),
        defaultCurrency = "SDG",
        defaultCountryCode = "SD",
        specializedModuleNumbers = setOf(16, 17, 27, 29),
    ),
    WORKSHOP(
        id = "workshop",
        title = BilingualText("ورش وصيانة", "Workshops & Repair"),
        summary = BilingualText(
            "أوامر الصيانة، قطع الغيار، فحص الاستلام، الضمان والعمالة",
            "Service orders, spare parts, intake inspection, technician labor"
        ),
        defaultCurrency = "SDG",
        defaultCountryCode = "SD",
        specializedModuleNumbers = setOf(16, 24, 28, 29),
    ),
    FACTORY(
        id = "factory",
        title = BilingualText("مصانع وإنتاج", "Manufacturing & Plants"),
        summary = BilingualText(
            "المواد الخام، قوائم التصنيع BOM، خطوط الإنتاج، تكلفة الوحدة",
            "Raw materials, bill of materials, production runs, unit costing"
        ),
        defaultCurrency = "SDG",
        defaultCountryCode = "SD",
        specializedModuleNumbers = setOf(16, 19, 23, 24, 25),
    ),
    SERVICES(
        id = "services",
        title = BilingualText("مؤسسات خدمية ومشاريع", "Services & Projects"),
        summary = BilingualText(
            "عقود المشاريع، مراكز التكلفة، الفوترة المرحلية، الميزانيات",
            "Project contracts, cost centers, milestone billing, budgets"
        ),
        defaultCurrency = "SDG",
        defaultCountryCode = "SD",
        specializedModuleNumbers = setOf(20, 22, 23, 24),
    );

    companion object {
        fun fromId(id: String): BusinessType =
            entries.firstOrNull { it.id == id } ?: RETAIL
    }
}

enum class InventoryCostingMethod(val id: String, val label: BilingualText) {
    WEIGHTED_AVERAGE(
        "wac",
        BilingualText("المتوسط المرجح", "Weighted Average Cost (WAC)")
    ),
    FIFO(
        "fifo",
        BilingualText("الوارد أولاً يُصرف أولاً (FIFO)", "First-In First-Out (FIFO)")
    );

    companion object {
        fun fromId(id: String): InventoryCostingMethod =
            entries.firstOrNull { it.id == id } ?: WEIGHTED_AVERAGE
    }
}
