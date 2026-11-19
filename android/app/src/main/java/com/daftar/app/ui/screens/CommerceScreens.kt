package com.daftar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.InventoryCostingMethod
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.domain.commerce.InvoiceLineRequest
import com.daftar.app.ui.components.*
import com.daftar.app.ui.theme.CrimsonError
import com.daftar.app.ui.theme.EmeraldProfit
import com.daftar.app.ui.viewmodel.WorkspaceSnapshot
import com.daftar.app.ui.viewmodel.WorkspaceViewModel
import java.math.BigDecimal

@Composable
fun SalesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot, onCreateInvoice: () -> Unit, onOpenPos: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val sales = snapshot.invoices.filter {
        (it.invoiceType == "SALE" || it.invoiceType == "SALE_RETURN") &&
            (searchQuery.isBlank() || it.invoiceNumber.contains(searchQuery, true) || it.notes.contains(searchQuery, true))
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (lang == AppLanguage.ARABIC) "المبيعات والفواتير (${sales.size})" else "Sales & Returns", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onOpenPos) { Text("POS") }
                Button(onClick = onCreateInvoice) { Text(if (lang == AppLanguage.ARABIC) "فاتورة" else "New") }
            }
        }
        OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text(if (lang == AppLanguage.ARABIC) "بحث برقم الفاتورة..." else "Search invoice...") }, modifier = Modifier.fillMaxWidth())
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        if (sales.isEmpty()) {
            EmptyStatePanel(if (lang == AppLanguage.ARABIC) "لا توجد فواتير مطابقة" else "No matching sales invoices", "")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sales) { inv ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${inv.invoiceNumber} (${inv.invoiceType})", fontWeight = FontWeight.Bold)
                                Text(inv.status, color = if (inv.status == "POSTED") EmeraldProfit else CrimsonError)
                            }
                            Text(
                                "${inv.issueDateIso} • ${MoneyMath.format(inv.totalOriginal, snapshot.decimals, inv.currencyCode)} (Base: ${MoneyMath.format(inv.totalBase, snapshot.decimals, snapshot.currency)})",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { vm.shareInvoicePdf(context, inv.id) }) {
                                    Text(if (lang == AppLanguage.ARABIC) "طباعة / مشاركة PDF" else "Print / Share PDF")
                                }
                                if (inv.invoiceType == "SALE" && inv.status == "POSTED") {
                                    TextButton(onClick = { vm.returnSaleInvoice(inv.id, "مرتجع عميل") }) {
                                        Text(if (lang == AppLanguage.ARABIC) "مرتجع مبيعات" else "Sale Return", color = CrimsonError)
                                    }
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
fun CreateInvoiceScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot, onDone: () -> Unit) {
    var selectedItem by remember(snapshot.items) { mutableStateOf(snapshot.items.firstOrNull()) }
    var qtyStr by remember { mutableStateOf("1") }
    var unitPriceStr by remember(selectedItem) { mutableStateOf(selectedItem?.salePriceBase?.toPlainString() ?: "1000") }
    var discountStr by remember { mutableStateOf("0") }
    var taxRateStr by remember { mutableStateOf(snapshot.taxRules.firstOrNull { it.isDefault }?.ratePercent?.toPlainString() ?: "0") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var currencyCode by remember { mutableStateOf(snapshot.currency) }
    var fxRateStr by remember { mutableStateOf("1") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "منشئ الفواتير والضرائب" else "Invoice Builder", style = MaterialTheme.typography.titleLarge)
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }

        if (snapshot.items.isNotEmpty()) {
            Text(if (lang == AppLanguage.ARABIC) "اختر الصنف:" else "Select Item:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                snapshot.items.take(4).forEach { item ->
                    FilterChip(selected = selectedItem?.id == item.id, onClick = { selectedItem = item }, label = { Text(item.nameAr) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(qtyStr, { qtyStr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الكمية" else "Qty") }, modifier = Modifier.weight(1f))
            OutlinedTextField(unitPriceStr, { unitPriceStr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "السعر" else "Unit Price") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(discountStr, { discountStr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "خصم" else "Discount") }, modifier = Modifier.weight(1f))
            OutlinedTextField(taxRateStr, { taxRateStr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الضريبة %" else "Tax %") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(currencyCode, { currencyCode = it.uppercase() }, label = { Text(if (lang == AppLanguage.ARABIC) "العملة" else "Currency") }, modifier = Modifier.weight(1f))
            OutlinedTextField(fxRateStr, { fxRateStr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "سعر الصرف" else "FX Rate") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CASH", "BANK", "CREDIT").forEach { pm ->
                FilterChip(selected = paymentMethod == pm, onClick = { paymentMethod = pm }, label = { Text(pm) })
            }
        }
        Button(onClick = {
            val item = selectedItem
            vm.issueSaleInvoice(
                partyId = snapshot.parties.firstOrNull()?.id,
                paymentMethod = paymentMethod,
                currencyCode = currencyCode,
                exchangeRate = MoneyMath.of(fxRateStr).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE,
                discountOrig = MoneyMath.of(discountStr),
                taxRate = MoneyMath.of(taxRateStr),
                lines = listOf(
                    InvoiceLineRequest(
                        itemId = item?.id,
                        itemName = item?.nameAr ?: "بند خدمة/بيع مباشر",
                        quantity = MoneyMath.of(qtyStr).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE,
                        unitPriceOriginal = MoneyMath.of(unitPriceStr),
                    )
                )
            )
            onDone()
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (lang == AppLanguage.ARABIC) "اعتماد وترحيل الفاتورة" else "Issue & Post Invoice")
        }
    }
}

@Composable
fun PosTerminalScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var barcodeSearch by remember { mutableStateOf("") }
    var cart by remember { mutableStateOf(listOf<Pair<String, BigDecimal>>()) }
    val itemsMap = snapshot.items.associateBy { it.id }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(if (lang == AppLanguage.ARABIC) "نقطة البيع السريعة POS" else "POS Cashier Terminal", "${snapshot.currency} • Offline Mode")
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        OutlinedTextField(barcodeSearch, { barcodeSearch = it }, label = { Text(if (lang == AppLanguage.ARABIC) "بحث باركود أو اسم الصنف" else "Barcode / SKU search") }, modifier = Modifier.fillMaxWidth())

        val filtered = snapshot.items.filter {
            barcodeSearch.isBlank() || it.barcode.contains(barcodeSearch, true) || it.nameAr.contains(barcodeSearch, true) || it.sku.contains(barcodeSearch, true)
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered) { item ->
                val stock = snapshot.itemStockQty(item.id)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.nameAr, fontWeight = FontWeight.Bold)
                            Text("${MoneyMath.format(item.salePriceBase, snapshot.decimals, snapshot.currency)} | Qty: $stock", style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalIconButton(onClick = { cart = cart + (item.id to BigDecimal.ONE) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
            }
        }

        val cartTotal = MoneyMath.sumOf(cart.map { (id, qty) ->
            val p = itemsMap[id]?.salePriceBase ?: BigDecimal.ZERO
            MoneyMath.multiply(p, qty, snapshot.decimals)
        })
        Button(
            onClick = {
                val reqs = cart.groupBy { it.first }.mapNotNull { (id, list) ->
                    val item = itemsMap[id] ?: return@mapNotNull null
                    InvoiceLineRequest(item.id, item.nameAr, quantity = MoneyMath.sumOf(list.map { it.second }), unitPriceOriginal = item.salePriceBase)
                }
                if (reqs.isNotEmpty()) {
                    vm.issueSaleInvoice(null, "CASH", snapshot.currency, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, reqs)
                    cart = emptyList()
                }
            },
            enabled = cart.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PointOfSale, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("${if (lang == AppLanguage.ARABIC) "تحصيل نقدي فوري" else "Checkout Cash"} (${MoneyMath.format(cartTotal, snapshot.decimals, snapshot.currency)})")
        }
    }
}

@Composable
fun PurchasesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var selectedItem by remember(snapshot.items) { mutableStateOf(snapshot.items.firstOrNull()) }
    var qty by remember { mutableStateOf("10") }
    var cost by remember { mutableStateOf("500") }
    var batchNo by remember { mutableStateOf("") }
    var expiryIso by remember { mutableStateOf("") }
    val purchases = snapshot.invoices.filter { it.invoiceType == "PURCHASE" || it.invoiceType == "PURCHASE_RETURN" }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "فواتير المشتريات والمرتجعات" else "Purchases & Returns", style = MaterialTheme.typography.titleLarge)
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (snapshot.items.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        snapshot.items.take(3).forEach { it ->
                            FilterChip(selected = selectedItem?.id == it.id, onClick = { selectedItem = it }, label = { Text(it.nameAr) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(qty, { qty = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الكمية" else "Qty") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(cost, { cost = it }, label = { Text(if (lang == AppLanguage.ARABIC) "سعر التكلفة" else "Unit Cost") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(batchNo, { batchNo = it }, label = { Text(if (lang == AppLanguage.ARABIC) "تشغيلة (اختياري)" else "Batch") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(expiryIso, { expiryIso = it }, label = { Text(if (lang == AppLanguage.ARABIC) "صلاحية YYYY-MM-DD" else "Expiry") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val item = selectedItem ?: return@Button
                    vm.issuePurchaseInvoice(
                        partyId = snapshot.parties.firstOrNull { it.partyType == "SUPPLIER" }?.id,
                        paymentMethod = "CREDIT",
                        currencyCode = snapshot.currency,
                        exchangeRate = BigDecimal.ONE,
                        taxRate = BigDecimal.ZERO,
                        lines = listOf(InvoiceLineRequest(item.id, item.nameAr, quantity = MoneyMath.of(qty), unitPriceOriginal = MoneyMath.of(cost))),
                        batchNumber = batchNo.ifBlank { null },
                        expiryDateIso = expiryIso.ifBlank { null },
                    )
                }, enabled = selectedItem != null) {
                    Text(if (lang == AppLanguage.ARABIC) "ترحيل فاتورة شراء للمخزن" else "Post Purchase Bill")
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(purchases) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${p.invoiceNumber} (${p.invoiceType}) • ${p.issueDateIso}")
                            Text(MoneyMath.format(p.totalBase, snapshot.decimals, snapshot.currency), fontWeight = FontWeight.Bold)
                        }
                        if (p.invoiceType == "PURCHASE" && p.status == "POSTED") {
                            TextButton(onClick = { vm.returnPurchaseInvoice(p.id, "مرتجع لمورد") }) {
                                Text(if (lang == AppLanguage.ARABIC) "مرتجع مشتريات" else "Return Purchase", color = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartiesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CUSTOMER") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "العملاء والموردون (${snapshot.parties.size})" else "Customers & Suppliers", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "CUSTOMER", onClick = { type = "CUSTOMER" }, label = { Text(if (lang == AppLanguage.ARABIC) "عميل" else "Customer") })
                    FilterChip(selected = type == "SUPPLIER", onClick = { type = "SUPPLIER" }, label = { Text(if (lang == AppLanguage.ARABIC) "مورد" else "Supplier") })
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الاسم" else "Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الهاتف" else "Phone") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (name.isNotBlank()) { vm.addParty(name, type, phone); name = ""; phone = "" } }) {
                    Text(if (lang == AppLanguage.ARABIC) "إضافة" else "Save")
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.parties) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${p.code} • ${p.name}", fontWeight = FontWeight.Bold)
                            Text("${p.partyType} • ${p.phone}", style = MaterialTheme.typography.labelMedium)
                        }
                        Text("${p.loyaltyPoints} pts")
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var nameAr by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var salePrice by remember { mutableStateOf("1500") }
    var costPrice by remember { mutableStateOf("1000") }
    var openQty by remember { mutableStateOf("20") }
    var whName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "المخزون والمستودعات (${snapshot.items.size})" else "Inventory & Warehouses", style = MaterialTheme.typography.titleLarge)
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryCostingMethod.entries.forEach { method ->
                FilterChip(
                    selected = snapshot.profile?.costingMethodId == method.id,
                    onClick = { vm.updateCostingMethod(method.id) },
                    label = { Text(method.label.get(lang)) },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (lang == AppLanguage.ARABIC) "إضافة صنف جديد برصيد افتتاحي" else "New Item", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(nameAr, { nameAr = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الصنف" else "Item Name") }, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(salePrice, { salePrice = it }, label = { Text(if (lang == AppLanguage.ARABIC) "سعر البيع" else "Sale") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(costPrice, { costPrice = it }, label = { Text(if (lang == AppLanguage.ARABIC) "التكلفة" else "Cost") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(openQty, { openQty = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الرصيد" else "Qty") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    if (nameAr.isNotBlank()) {
                        vm.addItem(sku, "", nameAr, nameAr, MoneyMath.of(salePrice), MoneyMath.of(costPrice), MoneyMath.of(openQty), false)
                        nameAr = ""; sku = ""
                    }
                }) { Text(if (lang == AppLanguage.ARABIC) "حفظ الصنف" else "Save Item") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(whName, { whName = it }, label = { Text(if (lang == AppLanguage.ARABIC) "مستودع جديد" else "New Warehouse") }, modifier = Modifier.weight(1f))
            Button(onClick = { if (whName.isNotBlank()) { vm.addWarehouse("WH", whName); whName = "" } }) { Text("+") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.items) { item ->
                val qty = snapshot.itemStockQty(item.id)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${item.sku} • ${item.nameAr}", fontWeight = FontWeight.Bold)
                            Text("Barcode: ${item.barcode} | Sale: ${MoneyMath.format(item.salePriceBase, snapshot.decimals, snapshot.currency)}", style = MaterialTheme.typography.labelMedium)
                        }
                        Text("$qty ${item.unitName}", fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeQrScreen(lang: AppLanguage, snapshot: WorkspaceSnapshot) {
    val firstItem = snapshot.items.firstOrNull()
    val firstInv = snapshot.invoices.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SectionHeaderCard(if (lang == AppLanguage.ARABIC) "مركز الباركود ورمز QR المحلي" else "Offline Barcode & QR Center", "Code-128 & QR Generation (No Cloud)")
        if (firstItem != null) {
            Text("${firstItem.nameAr} (${firstItem.barcode})", style = MaterialTheme.typography.titleMedium)
            OfflineCode128BarcodeImage(firstItem.barcode)
        } else {
            OfflineCode128BarcodeImage("DFTR-ITEM-001")
        }
        HorizontalDivider()
        val qrText = if (firstInv != null) {
            "DAFTAR|${snapshot.profile?.nameAr}|${firstInv.invoiceNumber}|${firstInv.totalBase} ${snapshot.currency}"
        } else {
            "DAFTAR|${snapshot.profile?.nameAr ?: "Vault"}|OFFLINE"
        }
        Text(if (lang == AppLanguage.ARABIC) "رمز QR للفاتورة/النشاط:" else "Document QR Code:", style = MaterialTheme.typography.titleMedium)
        OfflineQrCodeImage(qrText)
    }
}

@Composable
fun EmployeesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var name by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("50000") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "الموظفون والرواتب" else "Employees & Payroll", style = MaterialTheme.typography.titleLarge)
        snapshot.errorMessage?.let { StatusBanner(it, isError = true, onDismiss = { vm.clearFeedback() }) }
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الموظف" else "Employee") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(job, { job = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الوظيفة" else "Title") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(salary, { salary = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الراتب" else "Salary") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (name.isNotBlank()) { vm.addEmployee(name, job.ifBlank { "موظف" }, MoneyMath.of(salary)); name = "" } }) {
                        Text(if (lang == AppLanguage.ARABIC) "إضافة موظف" else "Add")
                    }
                    OutlinedButton(onClick = { vm.runMonthlyPayroll("2026-11") }, enabled = snapshot.employees.isNotEmpty()) {
                        Text(if (lang == AppLanguage.ARABIC) "ترحيل مسير رواتب" else "Run Payroll")
                    }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.employees) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${e.code} • ${e.fullName} (${e.jobTitle})")
                        Text(MoneyMath.format(e.baseSalaryBase, snapshot.decimals, snapshot.currency), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var accId by remember { mutableStateOf("5203") }
    var planned by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "الميزانيات التقديرية والتخطيط" else "Budgets & Variance", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(accId, { accId = it }, label = { Text("Account") }, modifier = Modifier.weight(1f))
            OutlinedTextField(planned, { planned = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المخطط" else "Planned") }, modifier = Modifier.weight(1f))
            Button(onClick = { if (planned.isNotBlank()) { vm.addBudget(accId, MoneyMath.of(planned)); planned = "" } }) { Text("+") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.budgets) { b ->
                val actual = snapshot.accountBalance(b.accountId)
                val ratio = if (b.plannedAmountBase > BigDecimal.ZERO) (actual.toFloat() / b.plannedAmountBase.toFloat()) else 0f
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${b.accountId} • Planned: ${MoneyMath.format(b.plannedAmountBase, snapshot.decimals)} | Actual: ${MoneyMath.format(actual, snapshot.decimals)}")
                        Spacer(Modifier.height(6.dp))
                        LedgerProportionBar("Execution", ratio, if (ratio > 1f) CrimsonError else EmeraldProfit)
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var title by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "المستندات والمرفقات المشفرة" else "Encrypted Document Archive", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(title, { title = it }, label = { Text(if (lang == AppLanguage.ARABIC) "عنوان المستند" else "Document Title") }, modifier = Modifier.weight(1f))
            Button(onClick = { if (title.isNotBlank()) { vm.addAttachmentRecord(title, "INVOICE", snapshot.invoices.firstOrNull()?.invoiceNumber ?: "DOC"); title = "" } }) {
                Text(if (lang == AppLanguage.ARABIC) "أرشفة" else "Archive")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.attachments) { a ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(a.title, fontWeight = FontWeight.Bold); Text("${a.linkedEntityType} • ${a.linkedEntityId}", style = MaterialTheme.typography.labelMedium) }
                        Text("SHA256:${a.sha256Hex.take(8)}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
