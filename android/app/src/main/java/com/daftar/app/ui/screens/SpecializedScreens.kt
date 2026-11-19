package com.daftar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.ui.components.EmptyStatePanel
import com.daftar.app.ui.components.SectionHeaderCard
import com.daftar.app.ui.components.StatusBanner
import com.daftar.app.ui.theme.CrimsonError
import com.daftar.app.ui.theme.EmeraldProfit
import com.daftar.app.ui.viewmodel.WorkspaceSnapshot
import com.daftar.app.ui.viewmodel.WorkspaceViewModel
import java.time.LocalDate

@Composable
fun BranchesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("الخرطوم") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "الفروع والمواقع (${snapshot.branches.size})" else "Branches & Locations", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(code, { code = it }, label = { Text("Code") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(name, { name = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الفرع" else "Branch") }, modifier = Modifier.weight(2f))
                }
                OutlinedTextField(city, { city = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المدينة" else "City") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (name.isNotBlank()) { vm.addBranch(code.ifBlank { "BR" }, name, city); code = ""; name = "" } }) {
                    Text(if (lang == AppLanguage.ARABIC) "إضافة فرع" else "Add Branch")
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.branches) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${b.code} • ${b.nameAr}", fontWeight = FontWeight.Bold)
                        Text(b.city)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var name by remember { mutableStateOf("") }
    var contractVal by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "المشاريع ومراكز التكلفة" else "Projects & Cost Centers", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم المشروع" else "Project") }, modifier = Modifier.weight(1.4f))
            OutlinedTextField(contractVal, { contractVal = it }, label = { Text(if (lang == AppLanguage.ARABIC) "القيمة" else "Contract") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = { if (name.isNotBlank()) { vm.addProject(name, MoneyMath.of(contractVal)); name = ""; contractVal = "" } }) {
            Text(if (lang == AppLanguage.ARABIC) "إضافة مشروع" else "Create Project")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.projects) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${p.code} • ${p.nameAr}", fontWeight = FontWeight.Bold); Text("CC: ${p.costCenterId}", style = MaterialTheme.typography.labelMedium) }
                        Text(MoneyMath.format(p.contractValueBase, snapshot.decimals, snapshot.currency))
                    }
                }
            }
        }
    }
}

@Composable
fun ManufacturingScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var qty by remember { mutableStateOf("10") }
    var unitCost by remember { mutableStateOf("800") }
    val firstItem = snapshot.items.firstOrNull()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(if (lang == AppLanguage.ARABIC) "المصانع وخطوط الإنتاج" else "Manufacturing & Production", "BOM & Unit Costing")
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(qty, { qty = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الكمية المنتجة" else "Qty") }, modifier = Modifier.weight(1f))
            OutlinedTextField(unitCost, { unitCost = it }, label = { Text(if (lang == AppLanguage.ARABIC) "تكلفة الوحدة" else "Unit Cost") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = { if (firstItem != null) vm.createProductionOrder(firstItem.id, MoneyMath.of(qty), MoneyMath.of(unitCost)) }, enabled = firstItem != null) {
            Text(if (lang == AppLanguage.ARABIC) "تنفيذ أمر إنتاج وترحيل المخزون" else "Execute Production Order")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.productionOrders) { po ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${po.orderNumber} (${po.quantityProduced} units)")
                        Text(MoneyMath.format(po.totalCostBase, snapshot.decimals, snapshot.currency), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var tableNo by remember { mutableStateOf("T-01") }
    var itemsTxt by remember { mutableStateOf("2x وجبة غداء") }
    var total by remember { mutableStateOf("6500") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(if (lang == AppLanguage.ARABIC) "المطاعم والطاولات والمطبخ" else "Restaurants & Kitchen Queue", "${snapshot.restaurantTables.size} tables • ${snapshot.kitchenOrders.size} KOT")
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(tableNo, { tableNo = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الطاولة" else "Table") }, modifier = Modifier.weight(1f))
            OutlinedTextField(total, { total = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الإجمالي" else "Total") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(itemsTxt, { itemsTxt = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الأصناف المطلوبة" else "Order Items") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.addRestaurantOrder(tableNo, itemsTxt, MoneyMath.of(total)) }) {
            Text(if (lang == AppLanguage.ARABIC) "إرسال طلب للمطبخ" else "Send to Kitchen")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.kitchenOrders) { ko ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${ko.orderNumber} • ${ko.tableId ?: ""}", fontWeight = FontWeight.Bold); Text(ko.itemsSummary, style = MaterialTheme.typography.labelMedium) }
                        Text("${ko.status} • ${MoneyMath.format(ko.totalOriginal, snapshot.decimals)}")
                    }
                }
            }
        }
    }
}

@Composable
fun PharmaciesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var drugName by remember { mutableStateOf("") }
    var batchNo by remember { mutableStateOf("B-2026") }
    var expiryIso by remember { mutableStateOf("2027-06-30") }
    var qty by remember { mutableStateOf("50") }
    val today = LocalDate.now().toString()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderCard(if (lang == AppLanguage.ARABIC) "الصيدليات والتشغيلات والصلاحية" else "Pharmacy FEFO & Expiry Control", "Hard block on expired batch sale")
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        OutlinedTextField(drugName, { drugName = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الدواء / المستحضر" else "Medication Name") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(batchNo, { batchNo = it }, label = { Text(if (lang == AppLanguage.ARABIC) "التشغيلة" else "Batch") }, modifier = Modifier.weight(1f))
            OutlinedTextField(expiryIso, { expiryIso = it }, label = { Text("YYYY-MM-DD") }, modifier = Modifier.weight(1.2f))
            OutlinedTextField(qty, { qty = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الكمية" else "Qty") }, modifier = Modifier.weight(0.8f))
        }
        Button(onClick = {
            if (drugName.isNotBlank()) {
                vm.addItem("MED-${System.currentTimeMillis() % 1000}", "", drugName, drugName, MoneyMath.of("1200"), MoneyMath.of("800"), MoneyMath.of(qty), true, batchNo, expiryIso)
                drugName = ""
            }
        }) { Text(if (lang == AppLanguage.ARABIC) "توريد تشغيلة دوائية" else "Add Pharmacy Batch") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.pharmacyBatches) { b ->
                val expired = b.expiryDateIso < today
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Batch ${b.batchNumber} • Exp: ${b.expiryDateIso}", fontWeight = FontWeight.Bold, color = if (expired) CrimsonError else EmeraldProfit)
                            Text("Remaining: ${b.quantityRemaining}", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(if (expired) "BLOCKED" else "ACTIVE", color = if (expired) CrimsonError else EmeraldProfit)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkshopsScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var asset by remember { mutableStateOf("") }
    var issue by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("2500") }
    var labor by remember { mutableStateOf("1500") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "الورش والصيانة وقطع الغيار" else "Workshops & Repair Orders", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        OutlinedTextField(asset, { asset = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المركبة / الجهاز" else "Vehicle / Device") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(issue, { issue = it }, label = { Text(if (lang == AppLanguage.ARABIC) "وصف العطل" else "Fault Summary") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(parts, { parts = it }, label = { Text(if (lang == AppLanguage.ARABIC) "قطع الغيار" else "Parts") }, modifier = Modifier.weight(1f))
            OutlinedTextField(labor, { labor = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المصنعية" else "Labor") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = { if (asset.isNotBlank()) { vm.addWorkshopJob(asset, issue, MoneyMath.of(parts), MoneyMath.of(labor)); asset = ""; issue = "" } }) {
            Text(if (lang == AppLanguage.ARABIC) "فتح أمر صيانة" else "Open Job Card")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.workshopJobs) { j ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${j.jobNumber} • ${j.assetDescription}", fontWeight = FontWeight.Bold); Text(j.issueSummary, style = MaterialTheme.typography.labelMedium) }
                        Text(MoneyMath.format(j.sparePartsCostBase.add(j.laborFeeBase), snapshot.decimals, snapshot.currency))
                    }
                }
            }
        }
    }
}

@Composable
fun ShippingScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var recipient by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("بورتسودان") }
    var cod by remember { mutableStateOf("12000") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "الشحن والتوصيل والتحصيل COD" else "Shipping & COD Delivery", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(recipient, { recipient = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المستلم" else "Recipient") }, modifier = Modifier.weight(1f))
            OutlinedTextField(city, { city = it }, label = { Text(if (lang == AppLanguage.ARABIC) "الوجهة" else "City") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(cod, { cod = it }, label = { Text(if (lang == AppLanguage.ARABIC) "مبلغ التحصيل COD" else "COD Amount") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (recipient.isNotBlank()) { vm.addShippingOrder(recipient, city, "مندوب محلي", MoneyMath.of(cod), MoneyMath.of("500")); recipient = "" } }) {
            Text(if (lang == AppLanguage.ARABIC) "إصدار بوليصة شحن" else "Create Waybill")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.shippingOrders) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${s.waybillNumber} • ${s.recipientName}", fontWeight = FontWeight.Bold); Text("${s.destinationCity} • ${s.status}", style = MaterialTheme.typography.labelMedium) }
                        Text(MoneyMath.format(s.codAmountBase, snapshot.decimals, snapshot.currency))
                    }
                }
            }
        }
    }
}

@Composable
fun LoyaltyScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var offerName by remember { mutableStateOf("") }
    var discPct by remember { mutableStateOf("10") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "العروض وبرامج ولاء العملاء" else "Offers & Loyalty Programs", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(offerName, { offerName = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم العرض" else "Campaign") }, modifier = Modifier.weight(1.4f))
            OutlinedTextField(discPct, { discPct = it }, label = { Text("%") }, modifier = Modifier.weight(0.6f))
        }
        Button(onClick = { if (offerName.isNotBlank()) { vm.addLoyaltyOffer(offerName, MoneyMath.of(discPct)); offerName = "" } }) {
            Text(if (lang == AppLanguage.ARABIC) "تفعيل عرض" else "Activate Offer")
        }
        if (snapshot.loyaltyOffers.isEmpty()) EmptyStatePanel(if (lang == AppLanguage.ARABIC) "لا توجد عروض نشطة" else "No active offers", "")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(snapshot.loyaltyOffers) { o ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(o.nameAr, fontWeight = FontWeight.Bold)
                        Text("${o.discountPercent}%")
                    }
                }
            }
        }
    }
}

@Composable
fun TaxesCurrenciesScreen(lang: AppLanguage, vm: WorkspaceViewModel, snapshot: WorkspaceSnapshot) {
    var curCode by remember { mutableStateOf("") }
    var curRate by remember { mutableStateOf("") }
    var taxName by remember { mutableStateOf("") }
    var taxRate by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (lang == AppLanguage.ARABIC) "تعدد العملات والضرائب القابلة للضبط" else "Currencies & Configurable Taxes", style = MaterialTheme.typography.titleLarge)
        snapshot.statusMessage?.let { StatusBanner(it, isSuccess = true, onDismiss = { vm.clearFeedback() }) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (lang == AppLanguage.ARABIC) "تحديث / إضافة عملة وسعر صرف" else "Currency & FX Rate", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(curCode, { curCode = it.uppercase() }, label = { Text("USD/SAR") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(curRate, { curRate = it }, label = { Text(if (lang == AppLanguage.ARABIC) "المقابل بـ ${snapshot.currency}" else "Rate") }, modifier = Modifier.weight(1.2f))
                }
                Button(onClick = { if (curCode.isNotBlank() && curRate.isNotBlank()) { vm.addCurrency(curCode, curCode, MoneyMath.of(curRate), 2); curCode = ""; curRate = "" } }) {
                    Text(if (lang == AppLanguage.ARABIC) "حفظ سعر الصرف (دون تعديل الماضي)" else "Update FX Rate")
                }
            }
        }
        snapshot.currencies.forEach { c ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${c.code} • ${c.nameAr} ${if (c.isBaseCurrency) "(Base)" else ""}")
                    Text("1 ${c.code} = ${c.exchangeRateToBase.toPlainString()} ${snapshot.currency}")
                }
            }
        }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(taxName, { taxName = it }, label = { Text(if (lang == AppLanguage.ARABIC) "اسم الضريبة" else "Tax Rule") }, modifier = Modifier.weight(1.4f))
            OutlinedTextField(taxRate, { taxRate = it }, label = { Text("%") }, modifier = Modifier.weight(0.6f))
        }
        Button(onClick = { if (taxName.isNotBlank()) { vm.addTaxRule(taxName, MoneyMath.of(taxRate)); taxName = ""; taxRate = "" } }) {
            Text(if (lang == AppLanguage.ARABIC) "إضافة ضريبة" else "Add Tax")
        }
    }
}
