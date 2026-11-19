package com.daftar.app.domain.audit

import com.daftar.app.core.model.BilingualText
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.domain.accounting.FinancialStatementsEngine
import java.math.BigDecimal
import java.time.LocalDate

enum class AuditSeverity { PASS, WARNING, CRITICAL }

data class DiagnosticFinding(
    val code: String,
    val title: BilingualText,
    val detail: BilingualText,
    val impact: BilingualText,
    val recommendation: BilingualText,
    val severity: AuditSeverity,
    val metricValue: String,
)

data class DiagnosticAuditReport(
    val generatedAtMillis: Long,
    val findings: List<DiagnosticFinding>,
    val criticalCount: Int,
    val warningCount: Int,
    val passCount: Int,
    val healthScorePercent: Int,
)

/**
 * 100% Read-only Diagnostic Audit Engine.
 * Audits 10 integrity, duplicate, treasury, inventory, expiry, relation, and security rules.
 * Explains cause, impact, and suggested fix. NEVER modifies database rows.
 */
class DiagnosticAuditEngine(private val db: ActivityDatabase) {

    suspend fun runFullDiagnostic(todayIso: String = LocalDate.now().toString()): DiagnosticAuditReport {
        val findings = mutableListOf<DiagnosticFinding>()
        val accDao = db.accountingDao()
        val invDao = db.inventoryDao()
        val comDao = db.commerceDao()
        val fs = FinancialStatementsEngine(db)

        // 1. Trial Balance Equilibrium
        val tb = fs.computeTrialBalance()
        findings.add(
            DiagnosticFinding(
                code = "AUD-01",
                title = BilingualText("توازن ميزان المراجعة", "Trial Balance Equilibrium"),
                detail = if (tb.isBalanced) {
                    BilingualText("مجموع المدين يطابق الدائن بدقة عشرية كاملة", "Total debits match credits exactly")
                } else {
                    BilingualText("عدم توازن بين إجمالي المدين والدائن في الأستاذ", "Debit and credit totals do not match")
                },
                impact = BilingualText("القوائم المالية والميزانية تتأثر بأي اختلال في الميزان", "Balance sheet parity depends on equilibrium"),
                recommendation = BilingualText("راجع القيود المذكورة في AUD-02 وأنشئ قيد تسوية عكسي", "Inspect AUD-02 and post documented adjustment"),
                severity = if (tb.isBalanced) AuditSeverity.PASS else AuditSeverity.CRITICAL,
                metricValue = "DR ${MoneyMath.format(tb.sumDebits)} / CR ${MoneyMath.format(tb.sumCredits)}",
            )
        )

        // 2. Per-Journal Entry Parity
        val entries = accDao.getAllJournalEntries()
        val linesByEntry = accDao.getAllJournalLines().groupBy { it.journalEntryId }
        val unbalancedCount = entries.count { entry ->
            if (entry.status == "DRAFT") false
            else {
                val lines = linesByEntry[entry.id].orEmpty()
                val dr = MoneyMath.sumOf(lines.map { it.debitBase })
                val cr = MoneyMath.sumOf(lines.map { it.creditBase })
                !MoneyMath.equalsExact(dr, cr)
            }
        }
        findings.add(
            DiagnosticFinding(
                code = "AUD-02",
                title = BilingualText("سلامة قيود اليومية الفردية", "Per-Journal DR=CR Parity"),
                detail = if (unbalancedCount == 0) {
                    BilingualText("جميع القيود المرحلة متوازنة بدون استثناء", "All posted entries are balanced")
                } else {
                    BilingualText("رُصدت قيود غير متوازنة في السجل", "Unbalanced posted entries detected")
                },
                impact = BilingualText("يُفسد تقارير الدخل وحسابات الأستاذ العام", "Distorts P&L and General Ledger balances"),
                recommendation = BilingualText("اعكس القيد غير المتوازن وأعد إدخاله بصورة متوازنة", "Reverse offending entry and repost balanced"),
                severity = if (unbalancedCount == 0) AuditSeverity.PASS else AuditSeverity.CRITICAL,
                metricValue = "$unbalancedCount errors",
            )
        )

        // 3. Unposted Drafts & Incomplete Invoices
        val draftCount = entries.count { it.status == "DRAFT" }
        val draftInvoices = comDao.getAllInvoices().count { it.status == "DRAFT" }
        findings.add(
            DiagnosticFinding(
                code = "AUD-03",
                title = BilingualText("القيود المسودة والفواتير غير المكتملة", "Unposted Drafts & Incomplete Invoices"),
                detail = BilingualText("عدد المسودات بانتظار الترحيل والاعتماد النهائي", "Draft journals/invoices awaiting final posting"),
                impact = BilingualText("لا تنعكس المستندات غير المرحلة في الأرباح والمخزون", "Drafts are excluded from P&L and inventory"),
                recommendation = BilingualText("راجع المسودات ثم رحّلها أو احذف المكرر منها", "Review drafts and post or discard obsolete ones"),
                severity = if (draftCount + draftInvoices > 0) AuditSeverity.WARNING else AuditSeverity.PASS,
                metricValue = "$draftCount JE / $draftInvoices INV",
            )
        )

        // 4. Negative Cash / Bank Balance Check
        val treasuries = comDao.getTreasuryAccounts()
        val negCashCount = treasuries.count { tr ->
            val lines = accDao.getPostedLinesForAccount(tr.linkedGlAccountId)
            val bal = MoneyMath.sumOf(lines.map { it.debitBase }).subtract(MoneyMath.sumOf(lines.map { it.creditBase }))
            bal < BigDecimal.ZERO
        }
        findings.add(
            DiagnosticFinding(
                code = "AUD-04",
                title = BilingualText("سلامة أرصدة الصناديق والبنوك", "Treasury Overdraft Check"),
                detail = if (negCashCount == 0) {
                    BilingualText("لا توجد خزائن أو حسابات بنكية برصيد سالب", "No negative cash/bank balances")
                } else {
                    BilingualText("تم رصد صرف نقدي يتجاوز الرصيد المتاح في الخزينة", "Cash/bank disbursements exceed available balance")
                },
                impact = BilingualText("يشير لسندات صرف غير مغطاة أو نسيان قيد إيداع", "Indicates unrecorded deposits or posting errors"),
                recommendation = BilingualText("تحقق من إيداعات الخزينة وأرصدة أول المدة", "Verify opening balances and deposit receipts"),
                severity = if (negCashCount == 0) AuditSeverity.PASS else AuditSeverity.CRITICAL,
                metricValue = "$negCashCount negative",
            )
        )

        // 5. Inventory GL vs Perpetual Stock Ledger Parity
        val invGlLines = accDao.getPostedLinesForAccount("1104")
        val invGlBalance = MoneyMath.sumOf(invGlLines.map { it.debitBase }).subtract(MoneyMath.sumOf(invGlLines.map { it.creditBase }))
        val stockTotalVal = MoneyMath.sumOf(invDao.getAllStocks().map { it.totalValueBase })
        val invDiff = invGlBalance.subtract(stockTotalVal).abs()
        val invMatches = invDiff <= BigDecimal("1.00")
        findings.add(
            DiagnosticFinding(
                code = "AUD-05",
                title = BilingualText("مطابقة المخزون الدفتري مع الأستاذ", "Inventory Perpetual vs GL"),
                detail = if (invMatches) {
                    BilingualText("قيمة بطاقات المخزون تطابق حساب المخزون بالأستاذ العام", "Stock valuation matches GL inventory asset")
                } else {
                    BilingualText("فارق بين جرد الأصناف والأستاذ العام", "Variance between item stocks and GL 1104")
                },
                impact = BilingualText("يؤثر على دقة تكلفة البضاعة المباعة والأصول", "Affects COGS and current assets accuracy"),
                recommendation = BilingualText("نفذ جرداً فعلياً أو طابق قيود الرصيد الافتتاحي", "Run stocktake audit and verify opening journals"),
                severity = if (invMatches) AuditSeverity.PASS else AuditSeverity.WARNING,
                metricValue = "Δ ${MoneyMath.format(invDiff)}",
            )
        )

        // 6. Negative Inventory Quantities
        val negStockCount = invDao.getAllStocks().count { it.quantityOnHand < BigDecimal.ZERO }
        findings.add(
            DiagnosticFinding(
                code = "AUD-06",
                title = BilingualText("فحص الأرصدة المخزنية السالبة", "Negative Stock Check"),
                detail = BilingualText("التأكد من عدم صرف كميات تتجاوز الرصيد المتاح", "Verify no negative item quantities exist"),
                impact = BilingualText("يؤدي لتعطل حساب تكلفة FIFO والمتوسط المرجح", "Breaks FIFO/WAC cost layer accuracy"),
                recommendation = BilingualText("أدخل فواتير المشتريات المفقودة أو عدّل الجرد", "Post missing purchase receipts or adjust stock"),
                severity = if (negStockCount == 0) AuditSeverity.PASS else AuditSeverity.CRITICAL,
                metricValue = "$negStockCount items",
            )
        )

        // 7. Pharmacy Expired & Expiring Soon Batches
        val batches = invDao.getAllPharmacyBatches().filter { it.quantityRemaining > BigDecimal.ZERO }
        val expiredCount = batches.count { it.expiryDateIso < todayIso }
        val soonDate = LocalDate.parse(todayIso).plusDays(30).toString()
        val expiringSoonCount = batches.count { it.expiryDateIso >= todayIso && it.expiryDateIso <= soonDate }
        findings.add(
            DiagnosticFinding(
                code = "AUD-07",
                title = BilingualText("التشغيلات المنتهية وقريبة الصلاحية", "Expired & Near-Expiry Batches"),
                detail = BilingualText("متابعة التشغيلات الصيدلانية والالتزام بقاعدة FEFO", "Monitors expired batches and 30-day FEFO window"),
                impact = BilingualText("النظام يمنع آلياً بيع أي تشغيلة منتهية الصلاحية", "Expired sales are automatically hard-blocked"),
                recommendation = BilingualText("أعدم أو أعِد للمورد التشغيلات المنتهية الموضحة", "Quarantine or return expired batches to supplier"),
                severity = when {
                    expiredCount > 0 -> AuditSeverity.CRITICAL
                    expiringSoonCount > 0 -> AuditSeverity.WARNING
                    else -> AuditSeverity.PASS
                },
                metricValue = "$expiredCount expired / $expiringSoonCount soon",
            )
        )

        // 8. Duplicate Customers & Relation Integrity (Invoice -> Journal)
        val invoices = comDao.getAllInvoices().filter { it.status != "DRAFT" }
        val unlinkedInvoices = invoices.count { it.journalEntryId.isNullOrBlank() }
        val duplicateParties = comDao.getActiveParties()
            .groupBy { it.name.trim().lowercase() }
            .count { it.value.size > 1 }
        findings.add(
            DiagnosticFinding(
                code = "AUD-08",
                title = BilingualText("تكرار العملاء وسلامة ربط الفواتير", "Duplicates & Relation Integrity"),
                detail = BilingualText("التحقق من ربط كل فاتورة بقيدها وعدم تكرار بطاقات العملاء", "Verifies invoice-journal foreign links and duplicate parties"),
                impact = BilingualText("تكرار العميل يشتت كشف حسابه والمديونيات", "Duplicate parties fragment customer statements"),
                recommendation = BilingualText("ادمج السجلات المكررة وتابع الفواتير غير المرحّلة", "Consolidate duplicate parties if flagged"),
                severity = when {
                    unlinkedInvoices > 0 -> AuditSeverity.CRITICAL
                    duplicateParties > 0 -> AuditSeverity.WARNING
                    else -> AuditSeverity.PASS
                },
                metricValue = "$unlinkedInvoices unlinked / $duplicateParties dup",
            )
        )

        // 9. Low Stock Reorder Alerts & Duplicate Invoices
        val items = invDao.getAllItems().associateBy { it.id }
        val lowStockCount = invDao.getAllStocks().count { stock ->
            val item = items[stock.itemId]
            item != null && !item.isServiceItem && stock.quantityOnHand <= item.reorderLevel
        }
        val dupInvGroups = invoices.groupBy { "${it.invoiceType}|${it.partyId}|${it.issueDateIso}|${it.totalBase.toPlainString()}" }
            .count { it.value.size > 1 }
        findings.add(
            DiagnosticFinding(
                code = "AUD-09",
                title = BilingualText("نواقص المخزون والفواتير المحتملة التكرار", "Low Stock & Potential Duplicate Invoices"),
                detail = BilingualText("أصناف بلغت حد الطلب وفواتير متطابقة بنفس اليوم والقيمة", "Items at reorder point and potential same-day duplicates"),
                impact = BilingualText("تضاعف الفواتير يضخم المبيعات أو المشتريات", "Duplicate invoices overstate ledger balances"),
                recommendation = BilingualText("تزود من الأصناف الناقصة وراجع أي فاتورة مكررة بعكسها", "Restock low items and reverse accidental duplicates"),
                severity = if (lowStockCount > 0 || dupInvGroups > 0) AuditSeverity.WARNING else AuditSeverity.PASS,
                metricValue = "$lowStockCount low / $dupInvGroups dupInv",
            )
        )

        // 10. Failed Auth Attempts & Database/Backup Integrity
        val securityFails = db.governanceDao().getAuditLogs().count { it.actionType == "LOGIN_FAILED" }
        findings.add(
            DiagnosticFinding(
                code = "AUD-10",
                title = BilingualText("سلامة الحماية ومحاولات الدخول", "Security & Failed Logins"),
                detail = BilingualText("رصد محاولات الدخول الفاشلة في سجل التدقيق غير القابل للتعديل", "Tracks failed PIN events in immutable audit log"),
                impact = BilingualText("تنبيه مبكر لمحاولات الوصول غير المصرح بها", "Early warning against unauthorized access attempts"),
                recommendation = BilingualText("راجع سجل التدقيق وقم بتصدير نسخة مشفرة دورية", "Inspect audit logs and export periodic SAF backups"),
                severity = if (securityFails > 3) AuditSeverity.WARNING else AuditSeverity.PASS,
                metricValue = "$securityFails events",
            )
        )

        val criticals = findings.count { it.severity == AuditSeverity.CRITICAL }
        val warnings = findings.count { it.severity == AuditSeverity.WARNING }
        val passes = findings.count { it.severity == AuditSeverity.PASS }
        val score = ((passes * 100 + warnings * 60) / findings.size.coerceAtLeast(1)).coerceIn(0, 100)

        return DiagnosticAuditReport(
            generatedAtMillis = System.currentTimeMillis(),
            findings = findings,
            criticalCount = criticals,
            warningCount = warnings,
            passCount = passes,
            healthScorePercent = score,
        )
    }
}
