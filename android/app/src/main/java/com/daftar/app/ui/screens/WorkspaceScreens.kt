package com.daftar.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftar.app.core.model.*
import com.daftar.app.ui.components.*
import com.daftar.app.ui.theme.*
import com.daftar.app.ui.viewmodel.AppUiState
import com.daftar.app.ui.viewmodel.AppViewModel
import com.daftar.app.ui.viewmodel.WorkspaceSnapshot
import com.daftar.app.ui.viewmodel.WorkspaceViewModel
import java.math.BigDecimal

@Composable
fun DashboardScreen(
    appViewModel: AppViewModel,
    uiState: AppUiState,
    snapshot: WorkspaceSnapshot,
    onNavigate: (ScreenRoute) -> Unit,
    onSwitchVault: () -> Unit,
) {
    val lang = uiState.language
    val currency = snapshot.currency
    val dec = snapshot.decimals
    val cashBal = snapshot.accountBalance("1101").add(snapshot.accountBalance("1102"))
    val revenue = snapshot.incomeStatement?.totalRevenue ?: BigDecimal.ZERO
    val expense = snapshot.incomeStatement?.totalExpense ?: BigDecimal.ZERO
    val netProfit = snapshot.incomeStatement?.netIncome ?: BigDecimal.ZERO
    val auditScore = snapshot.auditReport?.healthScorePercent ?: 100

    // One-time recovery key display modal right after activity creation
    val oneTimeKey = uiState.generatedRecoveryKeyOnce
    if (!oneTimeKey.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(if (lang == AppLanguage.ARABIC) "مفتاح الاسترداد السري (يُعرض مرة واحدة)" else "Vault Recovery Key (Shown Once)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (lang == AppLanguage.ARABIC) "احفظ هذا المفتاح في مكان آمن لاستعادة النسخ الاحتياطية المشفرة:"
                        else "Save this recovery key securely to decrypt future backups:"
                    )
                    Surface(color = GoldSoft, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = oneTimeKey,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyClassic,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { appViewModel.acknowledgeRecoveryKey() }) {
                    Text(if (lang == AppLanguage.ARABIC) "تم الحفظ بأمان" else "Saved Securely")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) snapshot.profile?.nameAr ?: "دفتر" else snapshot.profile?.nameEn ?: "DAFTAR",
            subtitle = "${snapshot.businessType.title.get(lang)} • ${snapshot.currency}",
            badge = if (lang == AppLanguage.ARABIC) "سلامة $auditScore%" else "Health $auditScore%",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OfflineEncryptionBadge(lang, appViewModel.container.isHardwareKeystoreActive)
            TextButton(onClick = onSwitchVault) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (lang == AppLanguage.ARABIC) "تبديل النشاط" else "Switch Vault")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DaftarKpiCard(
                title = if (lang == AppLanguage.ARABIC) "السيولة النقدية والبنكية" else "Cash & Bank",
                value = MoneyMath.format(cashBal, dec, currency),
                caption = "${snapshot.treasuries.size} ${if (lang == AppLanguage.ARABIC) "حسابات" else "accounts"}",
                accentColor = NavyClassic,
                modifier = Modifier.weight(1f),
            )
            DaftarKpiCard(
                title = if (lang == AppLanguage.ARABIC) "صافي الربح" else "Net Profit",
                value = MoneyMath.format(netProfit, dec, currency),
                caption = if (netProfit >= BigDecimal.ZERO) (if (lang == AppLanguage.ARABIC) "ربح تشغيلي" else "Profitable") else (if (lang == AppLanguage.ARABIC) "عجز" else "Deficit"),
                accentColor = if (netProfit >= BigDecimal.ZERO) EmeraldProfit else CrimsonError,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DaftarKpiCard(
                title = if (lang == AppLanguage.ARABIC) "الإيرادات" else "Revenues",
                value = MoneyMath.format(revenue, dec, currency),
                caption = "${snapshot.invoices.count { it.invoiceType == "SALE" }} ${if (lang == AppLanguage.ARABIC) "فواتير" else "invoices"}",
                accentColor = EmeraldProfit,
                modifier = Modifier.weight(1f),
            )
            DaftarKpiCard(
                title = if (lang == AppLanguage.ARABIC) "المصروفات والتكاليف" else "Expenses & COGS",
                value = MoneyMath.format(expense, dec, currency),
                caption = "${snapshot.journals.size} ${if (lang == AppLanguage.ARABIC) "قيود" else "journals"}",
                accentColor = CrimsonError,
                modifier = Modifier.weight(1f),
            )
        }

        FinancialComparisonChart(
            title = if (lang == AppLanguage.ARABIC) "ملخص الأداء المالي (الإيراد مقابل التكلفة)" else "Financial Performance (Revenue vs Cost)",
            primaryLabel = if (lang == AppLanguage.ARABIC) "الإيرادات والتدفق الداخل" else "Revenue / Inflow",
            secondaryLabel = if (lang == AppLanguage.ARABIC) "المصروفات والتدفق الخارج" else "Expenses / Outflow",
            data = listOf(
                BarChartDatum(if (lang == AppLanguage.ARABIC) "الدخل" else "P&L", revenue, expense),
                BarChartDatum(if (lang == AppLanguage.ARABIC) "الذمم" else "AR/AP", snapshot.accountBalance("1103").abs(), snapshot.accountBalance("2101").abs()),
                BarChartDatum(if (lang == AppLanguage.ARABIC) "النقدية" else "Cash", cashBal.max(BigDecimal.ZERO), snapshot.accountBalance("5101").abs()),
            ),
        )

        Text(
            text = if (lang == AppLanguage.ARABIC) "وصول سريع للوحدات:" else "Quick Actions:",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onNavigate(ScreenRoute.CREATE_INVOICE) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.ARABIC) "فاتورة" else "Invoice")
            }
            FilledTonalButton(onClick = { onNavigate(ScreenRoute.JOURNAL_ENTRIES) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Book, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.ARABIC) "قيد يومية" else "Journal")
            }
            FilledTonalButton(onClick = { onNavigate(ScreenRoute.REPORTS) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Assessment, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.ARABIC) "المراجعة" else "Audit")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onNavigate(ScreenRoute.AI_ASSISTANT) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Psychology, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.ARABIC) "المساعد المحلي" else "Smart Analyst")
            }
            OutlinedButton(onClick = { onNavigate(ScreenRoute.MAIN_MENU) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Apps, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.ARABIC) "كافة الوحدات" else "All Modules")
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    lang: AppLanguage,
    snapshot: WorkspaceSnapshot,
    onNavigate: (ScreenRoute) -> Unit,
) {
    val activeModules = remember(snapshot.businessType) {
        DaftarModulesRegistry.modulesForBusiness(snapshot.businessType)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionHeaderCard(
                title = if (lang == AppLanguage.ARABIC) "الوحدات المفعّلة للنشاط (${activeModules.size})" else "Enabled Activity Modules (${activeModules.size})",
                subtitle = snapshot.businessType.title.get(lang),
                badge = snapshot.currency,
            )
        }
        items(activeModules) { mod ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(mod.targetRoute) },
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${mod.number}. ${mod.title.get(lang)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = mod.targetRoute.subtitle.get(lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldPrimary)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    lang: AppLanguage,
    workspaceViewModel: WorkspaceViewModel,
    snapshot: WorkspaceSnapshot,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (lang == AppLanguage.ARABIC) "التنبيهات المحلية (${snapshot.notifications.size})" else "Local Alerts",
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = { workspaceViewModel.markAllNotificationsRead() }) {
                Text(if (lang == AppLanguage.ARABIC) "تعليم كمقروء" else "Mark Read")
            }
        }
        val auditAlerts = snapshot.auditReport?.findings?.filter { it.severity != com.daftar.app.domain.audit.AuditSeverity.PASS }.orEmpty()
        if (snapshot.notifications.isEmpty() && auditAlerts.isEmpty()) {
            EmptyStatePanel(
                title = if (lang == AppLanguage.ARABIC) "لا توجد تنبيهات معلقة" else "All clear",
                description = if (lang == AppLanguage.ARABIC) "النظام المحاسبي والمخزني سليم بالكامل" else "Ledger and stock thresholds look healthy",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(auditAlerts) { finding ->
                    StatusBanner(
                        message = "${finding.code}: ${finding.title.get(lang)} (${finding.metricValue})",
                        isError = finding.severity == com.daftar.app.domain.audit.AuditSeverity.CRITICAL,
                    )
                }
                items(snapshot.notifications) { notif ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (lang == AppLanguage.ARABIC) notif.titleAr else notif.titleEn, style = MaterialTheme.typography.titleMedium)
                            Text(if (lang == AppLanguage.ARABIC) notif.bodyAr else notif.bodyEn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
