package com.daftar.app.core.security

import com.daftar.app.core.model.BilingualText

enum class UserRole(val id: String, val label: BilingualText) {
    OWNER("owner", BilingualText("مالك النشاط", "Business Owner")),
    ACCOUNTANT("accountant", BilingualText("محاسب", "Accountant")),
    CASHIER("cashier", BilingualText("كاشير", "Cashier")),
    INVENTORY_MANAGER("inventory", BilingualText("أمين مخزن", "Inventory Manager")),
    AUDITOR("auditor", BilingualText("مراجع (قراءة فقط)", "Read-Only Auditor"));

    companion object {
        fun fromId(id: String): UserRole = entries.firstOrNull { it.id == id } ?: AUDITOR
    }
}

enum class RbacPermission(val code: String, val label: BilingualText) {
    VIEW("view", BilingualText("الاطلاع", "View")),
    CREATE("create", BilingualText("الإضافة", "Create")),
    EDIT("edit", BilingualText("التعديل", "Edit")),
    DELETE("delete", BilingualText("الحذف (غير المرحّل فقط)", "Delete (Drafts Only)")),
    POST("post", BilingualText("الترحيل والاعتماد", "Post & Approve")),
    REVERSE("reverse", BilingualText("القيد العكسي والتصحيح", "Create Reversal")),
    EXPORT("export", BilingualText("التصدير والنسخ الاحتياطي", "Export & Backup")),
    MANAGE_USERS("manage_users", BilingualText("إدارة المستخدمين", "Manage Users"));

    companion object {
        fun defaultsForRole(role: UserRole): Set<RbacPermission> = when (role) {
            UserRole.OWNER -> entries.toSet()
            UserRole.ACCOUNTANT -> setOf(VIEW, CREATE, EDIT, POST, REVERSE, EXPORT)
            UserRole.CASHIER -> setOf(VIEW, CREATE, POST)
            UserRole.INVENTORY_MANAGER -> setOf(VIEW, CREATE, EDIT)
            UserRole.AUDITOR -> setOf(VIEW, EXPORT)
        }

        fun serialize(perms: Set<RbacPermission>): String =
            perms.joinToString(",") { it.code }

        fun deserialize(raw: String): Set<RbacPermission> {
            val codes = raw.split(",").map { it.trim() }.toSet()
            return entries.filter { it.code in codes }.toSet()
        }
    }
}
