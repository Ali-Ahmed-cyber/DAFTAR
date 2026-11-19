package com.daftar.app.domain.assistant

import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.domain.accounting.FinancialStatementsEngine
import com.daftar.app.domain.audit.DiagnosticAuditEngine

data class AssistantReply(
    val query: String,
    val title: String,
    val summary: String,
    val bulletPoints: List<String>,
)

/**
 * 100% Offline, Deterministic, Read-Only Local Smart Accounting Assistant.
 * Uses local SQLCipher database facts and mathematical engines only.
 * Never calls external AI APIs and NEVER writes or modifies database rows.
 */
class LocalSmartAssistantEngine(
    private val db: ActivityDatabase,
    private val auditEngine: DiagnosticAuditEngine,
) {
    suspend fun answerQuery(rawQuery: String, language: AppLanguage): AssistantReply {
        val q = rawQuery.trim().lowercase()
        val fs = FinancialStatementsEngine(db)
        val profile = db.accountingDao().getProfile()
        val currency = profile?.baseCurrency ?: "SDG"
        val decimals = profile?.decimalPlaces ?: 2
        val pl = fs.computeIncomeStatement()
        val bs = fs.computeBalanceSheet()
        val cf = fs.computeCashFlows()
        val audit = auditEngine.runFullDiagnostic()

        return when {
            q.contains("ربح") || q.contains("دخل") || q.contains("profit") || q.contains("income") -> {
                AssistantReply(
                    query = rawQuery,
                    title = if (language == AppLanguage.ARABIC) "تحليل الربحية والدخل" else "Profitability Analysis",
                    summary = if (language == AppLanguage.ARABIC) {
                        "صافي الدخل الحالي هو ${MoneyMath.format(pl.netIncome, decimals, currency)} من إجمالي إيرادات ${MoneyMath.format(pl.totalRevenue, decimals, currency)}."
                    } else {
                        "Current net income is ${MoneyMath.format(pl.netIncome, decimals, currency)} on total revenue of ${MoneyMath.format(pl.totalRevenue, decimals, currency)}."
                    },
                    bulletPoints = listOf(
                        if (language == AppLanguage.ARABIC) "الإيرادات: ${MoneyMath.format(pl.totalRevenue, decimals, currency)}" else "Revenue: ${MoneyMath.format(pl.totalRevenue, decimals, currency)}",
                        if (language == AppLanguage.ARABIC) "المصروفات والتكاليف: ${MoneyMath.format(pl.totalExpense, decimals, currency)}" else "Expenses & COGS: ${MoneyMath.format(pl.totalExpense, decimals, currency)}",
                        if (language == AppLanguage.ARABIC) "عدد الفواتير المعتمدة: ${db.commerceDao().getAllInvoices().count { it.status != "DRAFT" }}" else "Posted Invoices: ${db.commerceDao().getAllInvoices().count { it.status != "DRAFT" }}",
                    )
                )
            }

            q.contains("سيول") || q.contains("نقد") || q.contains("بنك") || q.contains("cash") || q.contains("liquidity") -> {
                AssistantReply(
                    query = rawQuery,
                    title = if (language == AppLanguage.ARABIC) "تحليل السيولة والتدفق النقدي" else "Cash & Liquidity Position",
                    summary = if (language == AppLanguage.ARABIC) {
                        "إجمالي السيولة المتاحة (خزينة + بنك): ${MoneyMath.format(cf.endingCashBalance, decimals, currency)}."
                    } else {
                        "Total available liquidity (Cash + Bank): ${MoneyMath.format(cf.endingCashBalance, decimals, currency)}."
                    },
                    bulletPoints = listOf(
                        if (language == AppLanguage.ARABIC) "التدفق التشغيلي: ${MoneyMath.format(cf.operatingCashFlow, decimals, currency)}" else "Operating Cash Flow: ${MoneyMath.format(cf.operatingCashFlow, decimals, currency)}",
                        if (language == AppLanguage.ARABIC) "التدفق الاستثماري: ${MoneyMath.format(cf.investingCashFlow, decimals, currency)}" else "Investing Cash Flow: ${MoneyMath.format(cf.investingCashFlow, decimals, currency)}",
                        if (language == AppLanguage.ARABIC) "التدفق التمويلي: ${MoneyMath.format(cf.financingCashFlow, decimals, currency)}" else "Financing Cash Flow: ${MoneyMath.format(cf.financingCashFlow, decimals, currency)}",
                    )
                )
            }

            q.contains("خطأ") || q.contains("مراجع") || q.contains("تدقيق") || q.contains("audit") || q.contains("error") -> {
                AssistantReply(
                    query = rawQuery,
                    title = if (language == AppLanguage.ARABIC) "نتيجة الفحص المحاسبي المحلي" else "Local Audit Health Summary",
                    summary = if (language == AppLanguage.ARABIC) {
                        "درجة سلامة الدفاتر: ${audit.healthScorePercent}% (أخطاء حرجة: ${audit.criticalCount}، تنبيهات: ${audit.warningCount})."
                    } else {
                        "Ledger health score: ${audit.healthScorePercent}% (Critical: ${audit.criticalCount}, Warnings: ${audit.warningCount})."
                    },
                    bulletPoints = audit.findings.take(5).map {
                        "${it.code} ${it.title.get(language)}: ${it.metricValue}"
                    }
                )
            }

            else -> {
                AssistantReply(
                    query = rawQuery,
                    title = if (language == AppLanguage.ARABIC) "الملخص المالي الشامل (${profile?.nameAr ?: "دفتر"})" else "Financial Overview (${profile?.nameEn ?: "DAFTAR"})",
                    summary = if (language == AppLanguage.ARABIC) {
                        "الأصول: ${MoneyMath.format(bs.totalAssets, decimals, currency)} | صافي الربح: ${MoneyMath.format(pl.netIncome, decimals, currency)} | درجة المراجعة: ${audit.healthScorePercent}%."
                    } else {
                        "Assets: ${MoneyMath.format(bs.totalAssets, decimals, currency)} | Net Income: ${MoneyMath.format(pl.netIncome, decimals, currency)} | Audit Score: ${audit.healthScorePercent}%."
                    },
                    bulletPoints = listOf(
                        if (language == AppLanguage.ARABIC) "توازن الميزانية: ${if (bs.isBalanced) "متوازنة" else "تحتاج مراجعة"}" else "Balance Sheet: ${if (bs.isBalanced) "Balanced" else "Needs Review"}",
                        if (language == AppLanguage.ARABIC) "النقدية المتاحة: ${MoneyMath.format(cf.endingCashBalance, decimals, currency)}" else "Cash Balance: ${MoneyMath.format(cf.endingCashBalance, decimals, currency)}",
                        if (language == AppLanguage.ARABIC) "تنبيهات المراجعة الحرجة: ${audit.criticalCount}" else "Critical Audit Alerts: ${audit.criticalCount}",
                    )
                )
            }
        }
    }
}
