package com.daftar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.ui.components.DaftarKpiCard
import com.daftar.app.ui.components.EmptyStatePanel
import com.daftar.app.ui.components.SectionHeaderCard
import com.daftar.app.ui.components.StatusBanner
import com.daftar.app.ui.theme.CrimsonError
import com.daftar.app.ui.theme.EmeraldProfit
import com.daftar.app.ui.theme.GoldPrimary
import com.daftar.app.ui.viewmodel.WorkspaceSnapshot
import com.daftar.app.ui.viewmodel.WorkspaceViewModel
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun ChartOfAccountsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var code by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (lang == AppLanguage.ARABIC) "دليل الحسابات الهرمي" else "Chart of Accounts", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { showAdd = !showAdd }) {
                Text(if (lang == AppLanguage.ARABIC) "حساب فرعي" else "Add Account")
            }
        }
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        if (showAdd) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(code, { code = it }, label = { Text(if (lang == AppLanguage.ARABIC) "رمز الحساب (مثال 5210)" else "Code") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(nameAr, { nameAr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الحساب" else "Account Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.take(3)) })
                        }
                    }
                    Button(onClick = {
                        vm.addAccount(code, nameAr, nameAr, type, null)
                        code = ""; nameAr = ""; showAdd = false
                    }, enabled = code.isNotBlank() && nameAr.isNotBlank()) {
                        Text(if (lang == AppLanguage.ARABIC) "حفظ الحساب" else "Save")
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(snapshot.accounts) { acc ->
                val bal = snapshot.accountBalance(acc.id)
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${acc.code} • ${if (lang == AppLanguage.ARABIC) acc.nameAr else acc.nameEn}", fontWeight = if (acc.isGroup) FontWeight.Bold else FontWeight.Medium)
                            Text("${acc.type} (${acc.normalBalance})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!acc.isGroup) {
                            Text(MoneyMath.format(bal, snapshot.decimals, snapshot.currency), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalEntriesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    val postingAccounts = remember(snapshot.accounts) { snapshot.accounts.filter { !it.isGroup && it.isActive } }
    var debitId by remember(postingAccounts) { mutableStateOf(postingAccounts.firstOrNull { it.id == "5203" }?.id ?: "1101") }
    var creditId by remember(postingAccounts) { mutableStateOf(postingAccounts.firstOrNull { it.id == "1101" }?.id ?: "1102") }
    var amountStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var asDraft by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (lang == AppLanguage.ARABIC) "القيود اليومية (${snapshot.journals.size})" else "Journal Entries", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { showForm = !showForm }) { Text(if (lang == AppLanguage.ARABIC) "قيد جديد" else "New Entry") }
        }
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        if (showForm) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (lang == AppLanguage.ARABIC) "قيد مزدوج متوازن (المدين = الدائن)" else "Balanced Double-Entry", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(desc, { desc = it }, label = { Text(if (lang == AppLanguage.ARABIC) "البيان" else "Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amountStr, { amountStr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المبلغ (${snapshot.currency})" else "Amount") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(debitId, { debitId = it }, label = { Text(if (lang == AppLanguage.ARABIC) "حساب مدين (رمز)" else "Debit Acc") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(creditId, { creditId = it }, label = { Text(if (lang == AppLanguage.ARABIC) "حساب دائن (رمز)" else "Credit Acc") }, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = asDraft, onCheckedChange = { asDraft = it })
                        Text(if (lang == AppLanguage.ARABIC) "حفظ كمسودة (غير مرحّل)" else "Save as Draft")
                    }
                    Button(onClick = {
                        val amt = MoneyMath.of(amountStr)
                        if (amt > BigDecimal.ZERO) {
                            vm.postManualJournal(LocalDate.now().toString(), desc.ifBlank { "قيد يدوي" }, debitId, creditId, amt, snapshot.currency, BigDecimal.ONE, asDraft)
                            amountStr = ""; desc = ""; showForm = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (asDraft) (if (lang == AppLanguage.ARABIC) "حفظ مسودة" else "Save Draft") else (if (lang == AppLanguage.ARABIC) "ترحيل القيد" else "Post Entry"))
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(snapshot.journals) { entry ->
                val lines = snapshot.journalLines.filter { it.journalEntryId == entry.id }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${entry.entryNumber} • ${entry.transactionDateIso}", fontWeight = FontWeight.Bold)
                            Text(
                                text = entry.status,
                                color = when (entry.status) {
                                    "POSTED" -> EmeraldProfit
                                    "REVERSED" -> CrimsonError
                                    else -> GoldPrimary
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                        lines.forEach { l ->
                            Text(
                                "  [${l.accountId}] DR ${MoneyMath.format(l.debitBase, snapshot.decimals)} | CR ${MoneyMath.format(l.creditBase, snapshot.decimals)}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (entry.status == "DRAFT") {
                                TextButton(onClick = { vm.postDraftJournal(entry.id) }) { Text(if (lang == AppLanguage.ARABIC) "ترحيل" else "Post") }
                                TextButton(onClick = { vm.deleteJournal(entry.id) }) { Text(if (lang == AppLanguage.ARABIC) "حذف مسودة" else "Delete") }
                            } else if (entry.status == "POSTED") {
                                TextButton(onClick = { vm.reverseJournal(entry.id, "تصحيح موثق") }) {
                                    Text(if (lang == AppLanguage.ARABIC) "إنشاء قيد عكسي" else "Reverse Entry", color = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralLedgerScreen(lang: AppLanguage, snapshot: WorkspaceSnapshot) {
    var selectedAccId by remember { mutableStateOf("1101") }
    val leafAccounts = snapshot.accounts.filter { !it.isGroup }
    val postedIds = snapshot.journals.filter { it.status != "DRAFT" }.map { it.id }.toSet()
    val entriesMap = snapshot.journals.associateBy { it.id }
    val lines = snapshot.journalLines.filter { it.accountId == selectedAccId && it.journalEntryId in postedIds }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "دفتر الأستاذ العام" else "General Ledger", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(selectedAccId, { selectedAccId = it }, label = { Text(if (lang == AppLanguage.ARABIC) "رمز الحساب (مثال 1101، 4101، 1104)" else "Account ID") }, modifier = Modifier.fillMaxWidth())
        val bal = snapshot.accountBalance(selectedAccId)
        DaftarKpiCard(
            title = leafAccounts.firstOrNull { it.id == selectedAccId }?.let { if (lang == AppLanguage.ARABIC) it.nameAr else it.nameEn } ?: selectedAccId,
            value = MoneyMath.format(bal, snapshot.decimals, snapshot.currency),
            caption = "${lines.size} ${if (lang == AppLanguage.ARABIC) "حركات مرحلة" else "posted movements"}",
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(lines) { l ->
                val e = entriesMap[l.journalEntryId]
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${e?.entryNumber ?: ""} • ${e?.transactionDateIso ?: ""}", fontWeight = FontWeight.Bold)
                            Text(e?.description ?: l.memo, style = MaterialTheme.typography.labelMedium)
                        }
                        Text("DR ${MoneyMath.format(l.debitBase)} / CR ${MoneyMath.format(l.creditBase)}")
                    }
                }
            }
        }
    }
}

@Composable
fun TreasuryBanksScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var stmtBal by remember { mutableStateOf("") }
    var transferAmt by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "الخزائن والحسابات البنكية" else "Treasury & Bank Accounts",
            subtitle = if (lang == AppLanguage.ARABIC) "التحويلات الداخلية والتسوية البنكية" else "Internal transfers & bank reconciliation",
        )
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        snapshot.treasuries.forEach { tr ->
            val bal = snapshot.accountBalance(tr.linkedGlAccountId)
            DaftarKpiCard(
                title = if (lang == AppLanguage.ARABIC) tr.nameAr else tr.nameEn,
                value = MoneyMath.format(bal, snapshot.decimals, snapshot.currency),
                caption = "${tr.kind} • ${tr.lastReconciledDateIso ?: (if (lang == AppLanguage.ARABIC) "غير مسوّى" else "Unreconciled")}",
                accentColor = if (tr.kind == "BANK") GoldPrimary else EmeraldProfit,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (lang == AppLanguage.ARABIC) "تحويل بين الخزينة والبنك" else "Cash-to-Bank Transfer", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(transferAmt, { transferAmt = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المبلغ" else "Amount") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val amt = MoneyMath.of(transferAmt)
                    if (amt > BigDecimal.ZERO) {
                        vm.createVoucher("TREASURY_TRANSFER", "1102", "1101", amt, "إيداع نقدي من الخزينة للبنك")
                        transferAmt = ""
                    }
                }) { Text(if (lang == AppLanguage.ARABIC) "تحويل للبنك" else "Transfer to Bank") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (lang == AppLanguage.ARABIC) "التسوية البنكية" else "Bank Reconciliation", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(stmtBal, { stmtBal = it }, label = { Text(if (lang == AppLanguage.ARABIC) "رصيد كشف البنك" else "Bank Statement Balance") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    vm.reconcileBank("TR-BANK-MAIN", MoneyMath.of(stmtBal), "تسوية كشف حساب")
                    stmtBal = ""
                }) { Text(if (lang == AppLanguage.ARABIC) "حفظ التسوية" else "Save Reconciliation") }
            }
        }
    }
}

@Composable
fun ExpensesRevenuesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "المصروفات والإيرادات والسندات" else "Expenses, Income & Vouchers", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text(if (lang == AppLanguage.ARABIC) "سند مصروف" else "Expense") })
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text(if (lang == AppLanguage.ARABIC) "سند قبض/إيراد" else "Income") })
                }
                OutlinedTextField(desc, { desc = it }, label = { Text(if (lang == AppLanguage.ARABIC) "البيان" else "Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المبلغ (${snapshot.currency})" else "Amount") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val amt = MoneyMath.of(amount)
                    if (amt > BigDecimal.ZERO) {
                        if (type == "EXPENSE") vm.createVoucher("EXPENSE", "5203", "1101", amt, desc.ifBlank { "مصروف تشغيلي" })
                        else vm.createVoucher("INCOME", "1101", "4201", amt, desc.ifBlank { "إيراد متنوع" })
                        amount = ""; desc = ""
                    }
                }) { Text(if (lang == AppLanguage.ARABIC) "ترحيل السند" else "Post Voucher") }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.vouchers) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${v.voucherNumber} • ${v.voucherType}", fontWeight = FontWeight.Bold)
                            Text(v.description, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(MoneyMath.format(v.amountBase, snapshot.decimals, snapshot.currency), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FixedAssetsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var salvage by remember { mutableStateOf("0") }
    var months by remember { mutableStateOf("60") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "الأصول الثابتة والإهلاك" else "Fixed Assets & Depreciation", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الأصل" else "Asset Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cost, { cost = it }, label = { Text(if (lang == AppLanguage.ARABIC) "التكلفة" else "Cost") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(salvage, { salvage = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الخردة" else "Salvage") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(months, { months = it }, label = { Text(if (lang == AppLanguage.ARABIC) "العمر(شهر)" else "Months") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val c = MoneyMath.of(cost)
                    if (c > BigDecimal.ZERO && name.isNotBlank()) {
                        vm.addFixedAsset(name, c, MoneyMath.of(salvage), months.toIntOrNull() ?: 60)
                        name = ""; cost = ""
                    }
                }) { Text(if (lang == AppLanguage.ARABIC) "تسجيل الأصل" else "Register Asset") }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(snapshot.fixedAssets) { a ->
                val nbv = a.acquisitionCostBase.subtract(a.accumulatedDepreciationBase)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${a.assetCode} • ${a.nameAr}", fontWeight = FontWeight.Bold)
                            Text("NBV: ${MoneyMath.format(nbv, snapshot.decimals, snapshot.currency)} | Depr: ${MoneyMath.format(a.accumulatedDepreciationBase)}", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(onClick = { vm.runMonthlyDepreciation(a.id) }) {
                            Text(if (lang == AppLanguage.ARABIC) "إهلاك شهر" else "Depreciate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrialBalanceScreen(lang: AppLanguage, snapshot: WorkspaceSnapshot) {
    val tb = snapshot.trialBalance
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "ميزان المراجعة" else "Trial Balance",
            subtitle = if (tb?.isBalanced == true) (if (lang == AppLanguage.ARABIC) "متوازن ١٠٠٪ (المدين = الدائن)" else "100% Balanced") else "Check parity",
            badge = if (tb?.isBalanced == true) "PASS" else "ALERT",
        )
        if (tb == null || tb.rows.isEmpty()) {
            EmptyStatePanel(if (lang == AppLanguage.ARABIC) "لا توجد قيود مرحلة بعد" else "No posted balances", "")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DaftarKpiCard(if (lang == AppLanguage.ARABIC) "إجمالي المدين" else "Total Debits", MoneyMath.format(tb.sumDebits, snapshot.decimals, snapshot.currency), "DR", EmeraldProfit, Modifier.weight(1f))
                DaftarKpiCard(if (lang == AppLanguage.ARABIC) "إجمالي الدائن" else "Total Credits", MoneyMath.format(tb.sumCredits, snapshot.decimals, snapshot.currency), "CR", GoldPrimary, Modifier.weight(1f))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tb.rows) { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${r.account.code} ${if (lang == AppLanguage.ARABIC) r.account.nameAr else r.account.nameEn}", Modifier.weight(1f))
                            Text("DR ${MoneyMath.format(r.netDebitBalance)} | CR ${MoneyMath.format(r.netCreditBalance)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialStatementsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    val pl = snapshot.incomeStatement
    val bs = snapshot.balanceSheet
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "القوائم المالية وإقفال الفترة" else "Financial Statements & Closing",
            subtitle = if (lang == AppLanguage.ARABIC) "قائمة الدخل والميزانية العمومية" else "Income Statement & Balance Sheet",
        )
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (lang == AppLanguage.ARABIC) "قائمة الدخل (الأرباح والخسائر)" else "Income Statement", style = MaterialTheme.typography.titleMedium)
                Text("${if (lang == AppLanguage.ARABIC) "إجمالي الإيرادات:" else "Total Revenue:"} ${MoneyMath.format(pl?.totalRevenue ?: BigDecimal.ZERO, snapshot.decimals, snapshot.currency)}")
                Text("${if (lang == AppLanguage.ARABIC) "إجمالي المصروفات:" else "Total Expenses:"} ${MoneyMath.format(pl?.totalExpense ?: BigDecimal.ZERO, snapshot.decimals, snapshot.currency)}")
                HorizontalDivider()
                Text(
                    "${if (lang == AppLanguage.ARABIC) "صافي الدخل:" else "Net Income:"} ${MoneyMath.format(pl?.netIncome ?: BigDecimal.ZERO, snapshot.decimals, snapshot.currency)}",
                    fontWeight = FontWeight.Bold,
                    color = if ((pl?.netIncome ?: BigDecimal.ZERO) >= BigDecimal.ZERO) EmeraldProfit else CrimsonError,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (lang == AppLanguage.ARABIC) "الميزانية العمومية (المركز المالي)" else "Balance Sheet", style = MaterialTheme.typography.titleMedium)
                Text("${if (lang == AppLanguage.ARABIC) "إجمالي الأصول:" else "Total Assets:"} ${MoneyMath.format(bs?.totalAssets ?: BigDecimal.ZERO, snapshot.decimals, snapshot.currency)}")
                Text("${if (lang == AppLanguage.ARABIC) "الالتزامات + حقوق الملكية:" else "Liabilities + Equity:"} ${MoneyMath.format(bs?.totalLiabilitiesAndEquity ?: BigDecimal.ZERO, snapshot.decimals, snapshot.currency)}")
                Text(
                    text = if (bs?.isBalanced == true) (if (lang == AppLanguage.ARABIC) "المعادلة المحاسبية متوازنة (الأصول = الالتزامات + الملكية)" else "Accounting Equation Balanced") else "Mismatch",
                    color = if (bs?.isBalanced == true) EmeraldProfit else CrimsonError,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Text(if (lang == AppLanguage.ARABIC) "الفترات المالية:" else "Fiscal Periods:", style = MaterialTheme.typography.titleMedium)
        snapshot.fiscalPeriods.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(p.nameAr, fontWeight = FontWeight.Bold)
                        Text("${p.startDateIso} -> ${p.endDateIso} | ${if (p.isClosed) "مقفلة" else "مفتوحة"}", style = MaterialTheme.typography.labelMedium)
                    }
                    if (!p.isClosed) {
                        Button(onClick = { vm.closePeriod(p.id) }) {
                            Text(if (lang == AppLanguage.ARABIC) "إقفال الفترة" else "Close Period")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowsScreen(lang: AppLanguage, snapshot: WorkspaceSnapshot) {
    val cf = snapshot.cashFlow
    val dec = snapshot.decimals
    val cur = snapshot.currency
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderCard(
            title = if (lang == AppLanguage.ARABIC) "قائمة التدفقات النقدية" else "Cash Flow Statement",
            subtitle = if (lang == AppLanguage.ARABIC) "التشغيلي والاستثماري والتمويلي" else "Operating, Investing & Financing",
        )
        DaftarKpiCard(if (lang == AppLanguage.ARABIC) "التدفق التشغيلي" else "Operating", MoneyMath.format(cf?.operatingCashFlow ?: BigDecimal.ZERO, dec, cur), "Operating")
        DaftarKpiCard(if (lang == AppLanguage.ARABIC) "التدفق الاستثماري" else "Investing", MoneyMath.format(cf?.investingCashFlow ?: BigDecimal.ZERO, dec, cur), "Investing")
        DaftarKpiCard(if (lang == AppLanguage.ARABIC) "التدفق التمويلي" else "Financing", MoneyMath.format(cf?.financingCashFlow ?: BigDecimal.ZERO, dec, cur), "Financing")
        DaftarKpiCard(if (lang == AppLanguage.ARABIC) "رصيد النقدية الختامي" else "Ending Cash", MoneyMath.format(cf?.endingCashBalance ?: BigDecimal.ZERO, dec, cur), "1101 + 1102", EmeraldProfit)
    }
}
