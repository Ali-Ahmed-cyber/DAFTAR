package com.daftar.app.domain.commerce

import androidx.room.withTransaction
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.*
import com.daftar.app.domain.accounting.DoubleEntryEngine
import com.daftar.app.domain.accounting.JournalLineDraft
import com.daftar.app.domain.inventory.InventoryValuationEngine
import java.math.BigDecimal
import java.util.UUID

data class InvoiceLineRequest(
    val itemId: String?,
    val itemName: String,
    val batchId: String? = null,
    val quantity: BigDecimal,
    val unitPriceOriginal: BigDecimal,
    val taxRatePercent: BigDecimal = BigDecimal.ZERO,
)

class CommerceTransactionEngine(
    private val db: ActivityDatabase,
    private val doubleEntry: DoubleEntryEngine,
    private val inventory: InventoryValuationEngine,
) {
    suspend fun postSaleInvoice(
        issueDateIso: String,
        partyId: String?,
        branchId: String = "BR-01",
        warehouseId: String = "WH-01",
        treasuryAccountId: String = "TR-CASH-MAIN",
        paymentMethod: String = "CASH", // CASH, BANK, CREDIT
        currencyCode: String = "SDG",
        exchangeRateAtTx: BigDecimal = BigDecimal.ONE,
        discountOriginal: BigDecimal = BigDecimal.ZERO,
        lines: List<InvoiceLineRequest>,
        notes: String = "",
        costCenterId: String? = "CC-MAIN",
        userId: String = "system",
        userName: String = "System",
    ): InvoiceEntity {
        require(lines.isNotEmpty()) { "Invoice must contain at least one line" }
        val profile = db.accountingDao().getProfile()
        val decimals = profile?.decimalPlaces ?: 2

        return db.withTransaction {
            val invNum = doubleEntry.nextSequenceNumber("INV-S")
            val invoiceId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val subOrig = MoneyMath.sumOf(lines.map { MoneyMath.multiply(it.quantity, it.unitPriceOriginal, decimals) })
            val discOrig = MoneyMath.scaleMoney(discountOriginal.max(BigDecimal.ZERO), decimals)
            val taxableBaseFactor = if (subOrig > BigDecimal.ZERO) {
                MoneyMath.divide(subOrig.subtract(discOrig).max(BigDecimal.ZERO), subOrig, 6)
            } else BigDecimal.ONE

            var taxOrig = BigDecimal.ZERO
            val lineEntities = lines.map { req ->
                val grossOrig = MoneyMath.multiply(req.quantity, req.unitPriceOriginal, decimals)
                val netLineOrig = MoneyMath.multiply(grossOrig, taxableBaseFactor, decimals)
                val lineTaxOrig = MoneyMath.percentage(netLineOrig, req.taxRatePercent, decimals)
                taxOrig = taxOrig.add(lineTaxOrig)

                InvoiceLineEntity(
                    id = UUID.randomUUID().toString(),
                    invoiceId = invoiceId,
                    itemId = req.itemId,
                    itemNameSnapshot = req.itemName,
                    batchId = req.batchId,
                    quantity = req.quantity,
                    unitPriceOriginal = req.unitPriceOriginal,
                    taxRatePercent = req.taxRatePercent,
                    taxAmountOriginal = lineTaxOrig,
                    lineTotalOriginal = netLineOrig.add(lineTaxOrig),
                    lineTotalBase = MoneyMath.multiply(netLineOrig.add(lineTaxOrig), exchangeRateAtTx, decimals),
                )
            }

            val totalOrig = (subOrig.subtract(discOrig)).add(taxOrig)
            val subBase = MoneyMath.multiply(subOrig, exchangeRateAtTx, decimals)
            val discBase = MoneyMath.multiply(discOrig, exchangeRateAtTx, decimals)
            val taxBase = MoneyMath.multiply(taxOrig, exchangeRateAtTx, decimals)
            val totalBase = MoneyMath.multiply(totalOrig, exchangeRateAtTx, decimals)

            // Deduct stock and calculate true COGS (with pharmacy FEFO expiry checks)
            var totalCogsBase = BigDecimal.ZERO
            val updatedLines = lineEntities.map { l ->
                if (l.itemId != null) {
                    val lineCogs = inventory.consumeStock(
                        itemId = l.itemId,
                        warehouseId = warehouseId,
                        quantity = l.quantity,
                        saleDateIso = issueDateIso,
                        sourceRef = invNum,
                        explicitBatchId = l.batchId,
                    )
                    totalCogsBase = totalCogsBase.add(lineCogs)
                    val unitCostSnap = if (l.quantity > BigDecimal.ZERO) MoneyMath.divide(lineCogs, l.quantity, 4) else BigDecimal.ZERO
                    l.copy(unitCostBaseSnapshot = unitCostSnap)
                } else l
            }

            val debitAccountId = when (paymentMethod) {
                "CREDIT" -> "1103" // Accounts Receivable
                else -> db.commerceDao().getTreasuryById(treasuryAccountId)?.linkedGlAccountId ?: "1101"
            }

            val journalDrafts = mutableListOf<JournalLineDraft>()
            journalDrafts.add(
                JournalLineDraft(
                    accountId = debitAccountId,
                    debitBase = totalBase,
                    originalCurrency = currencyCode,
                    originalAmount = totalOrig,
                    exchangeRateAtTx = exchangeRateAtTx,
                    costCenterId = costCenterId,
                    branchId = branchId,
                    partyId = partyId,
                    memo = "فاتورة مبيعات $invNum",
                )
            )
            if (discBase > BigDecimal.ZERO) {
                journalDrafts.add(
                    JournalLineDraft(
                        accountId = "4103",
                        debitBase = discBase,
                        originalCurrency = currencyCode,
                        originalAmount = discOrig,
                        exchangeRateAtTx = exchangeRateAtTx,
                        costCenterId = costCenterId,
                        memo = "خصم مسموح به $invNum",
                    )
                )
            }
            journalDrafts.add(
                JournalLineDraft(
                    accountId = "4101",
                    creditBase = subBase,
                    originalCurrency = currencyCode,
                    originalAmount = subOrig,
                    exchangeRateAtTx = exchangeRateAtTx,
                    costCenterId = costCenterId,
                    branchId = branchId,
                    partyId = partyId,
                    memo = "إيراد مبيعات $invNum",
                )
            )
            if (taxBase > BigDecimal.ZERO) {
                journalDrafts.add(
                    JournalLineDraft(
                        accountId = "2102",
                        creditBase = taxBase,
                        originalCurrency = currencyCode,
                        originalAmount = taxOrig,
                        exchangeRateAtTx = exchangeRateAtTx,
                        costCenterId = costCenterId,
                        memo = "ضريبة المبيعات $invNum",
                    )
                )
            }
            if (totalCogsBase > BigDecimal.ZERO) {
                journalDrafts.add(
                    JournalLineDraft(
                        accountId = "5101",
                        debitBase = totalCogsBase,
                        costCenterId = costCenterId,
                        memo = "تكلفة بضاعة مباعة $invNum",
                    )
                )
                journalDrafts.add(
                    JournalLineDraft(
                        accountId = "1104",
                        creditBase = totalCogsBase,
                        costCenterId = costCenterId,
                        memo = "صرف مخزون $invNum",
                    )
                )
            }

            val journalEntry = doubleEntry.postJournalEntry(
                transactionDateIso = issueDateIso,
                description = "قيد فاتورة مبيعات $invNum",
                sourceModule = "SALES",
                sourceDocumentId = invoiceId,
                currencyCode = currencyCode,
                exchangeRateAtTx = exchangeRateAtTx,
                lines = journalDrafts,
                userId = userId,
                userName = userName,
            )

            val invoice = InvoiceEntity(
                id = invoiceId,
                invoiceNumber = invNum,
                invoiceType = "SALE",
                status = "POSTED",
                issueDateIso = issueDateIso,
                recordedAtMillis = now,
                partyId = partyId,
                branchId = branchId,
                warehouseId = warehouseId,
                treasuryAccountId = treasuryAccountId,
                paymentMethod = paymentMethod,
                currencyCode = currencyCode,
                exchangeRateAtTx = MoneyMath.scaleRate(exchangeRateAtTx),
                subtotalOriginal = subOrig,
                discountOriginal = discOrig,
                taxOriginal = taxOrig,
                totalOriginal = totalOrig,
                subtotalBase = subBase,
                discountBase = discBase,
                taxBase = taxBase,
                totalBase = totalBase,
                paidBase = if (paymentMethod == "CREDIT") BigDecimal.ZERO else totalBase,
                cogsBase = totalCogsBase,
                journalEntryId = journalEntry.id,
                notes = notes,
            )

            db.commerceDao().insertInvoice(invoice)
            db.commerceDao().insertInvoiceLines(updatedLines)

            // Update customer loyalty points if applicable (1 point per 1000 base unit)
            if (partyId != null) {
                val party = db.commerceDao().getPartyById(partyId)
                if (party != null) {
                    val ptsEarned = totalBase.divideToIntegralValue(BigDecimal("1000")).toInt()
                    if (ptsEarned > 0) {
                        db.commerceDao().upsertParty(party.copy(loyaltyPoints = party.loyaltyPoints + ptsEarned))
                    }
                }
            }

            invoice
        }
    }

    suspend fun postSaleReturn(
        originalInvoiceId: String,
        returnDateIso: String,
        reason: String,
        userId: String = "system",
        userName: String = "System",
    ): InvoiceEntity {
        val comDao = db.commerceDao()
        val original = comDao.getInvoiceById(originalInvoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $originalInvoiceId")
        require(original.invoiceType == "SALE") { "Only SALE invoices can have a sale return" }
        require(original.status == "POSTED" && original.reversedByInvoiceId == null) {
            "Invoice ${original.invoiceNumber} is already returned or reversed"
        }

        val origLines = comDao.getLinesForInvoice(originalInvoiceId)
        return db.withTransaction {
            val retNum = doubleEntry.nextSequenceNumber("RET-S")
            val retId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // Restore inventory using locked historical unitCostBaseSnapshot
            for (l in origLines) {
                if (l.itemId != null) {
                    val item = db.inventoryDao().getItemById(l.itemId)
                    if (item != null && !item.isServiceItem) {
                        val restoreCost = if (l.unitCostBaseSnapshot > BigDecimal.ZERO) {
                            l.unitCostBaseSnapshot
                        } else item.defaultPurchaseCostBase
                        inventory.receiveStock(
                            itemId = l.itemId,
                            warehouseId = original.warehouseId,
                            quantity = l.quantity,
                            unitCostBase = restoreCost,
                            sourceRef = retNum,
                        )
                    }
                }
            }

            val creditAccountId = when (original.paymentMethod) {
                "CREDIT" -> "1103"
                else -> comDao.getTreasuryById(original.treasuryAccountId)?.linkedGlAccountId ?: "1101"
            }

            val drafts = mutableListOf<JournalLineDraft>()
            drafts.add(
                JournalLineDraft(
                    accountId = "4102",
                    debitBase = original.subtotalBase,
                    originalCurrency = original.currencyCode,
                    originalAmount = original.subtotalOriginal,
                    exchangeRateAtTx = original.exchangeRateAtTx,
                    partyId = original.partyId,
                    memo = "مرتجع مبيعات ${original.invoiceNumber}",
                )
            )
            if (original.taxBase > BigDecimal.ZERO) {
                drafts.add(
                    JournalLineDraft(
                        accountId = "2102",
                        debitBase = original.taxBase,
                        originalCurrency = original.currencyCode,
                        originalAmount = original.taxOriginal,
                        exchangeRateAtTx = original.exchangeRateAtTx,
                        memo = "عكس ضريبة مبيعات ${original.invoiceNumber}",
                    )
                )
            }
            if (original.discountBase > BigDecimal.ZERO) {
                drafts.add(
                    JournalLineDraft(
                        accountId = "4103",
                        creditBase = original.discountBase,
                        memo = "عكس خصم مسموح به ${original.invoiceNumber}",
                    )
                )
            }
            drafts.add(
                JournalLineDraft(
                    accountId = creditAccountId,
                    creditBase = original.totalBase,
                    originalCurrency = original.currencyCode,
                    originalAmount = original.totalOriginal,
                    exchangeRateAtTx = original.exchangeRateAtTx,
                    partyId = original.partyId,
                    memo = "رد قيمة فاتورة ${original.invoiceNumber}",
                )
            )
            if (original.cogsBase > BigDecimal.ZERO) {
                drafts.add(
                    JournalLineDraft(
                        accountId = "1104",
                        debitBase = original.cogsBase,
                        memo = "إرجاع مخزون بتكلفة ${original.invoiceNumber}",
                    )
                )
                drafts.add(
                    JournalLineDraft(
                        accountId = "5101",
                        creditBase = original.cogsBase,
                        memo = "عكس تكلفة مبيعات ${original.invoiceNumber}",
                    )
                )
            }

            val entry = doubleEntry.postJournalEntry(
                transactionDateIso = returnDateIso,
                description = "قيد مرتجع مبيعات $retNum عن ${original.invoiceNumber} — $reason",
                sourceModule = "SALES_RETURN",
                sourceDocumentId = retId,
                currencyCode = original.currencyCode,
                exchangeRateAtTx = original.exchangeRateAtTx,
                lines = drafts,
                userId = userId,
                userName = userName,
                reversalOfEntryId = original.journalEntryId,
            )

            val returnInvoice = InvoiceEntity(
                id = retId,
                invoiceNumber = retNum,
                invoiceType = "SALE_RETURN",
                status = "POSTED",
                issueDateIso = returnDateIso,
                recordedAtMillis = now,
                partyId = original.partyId,
                branchId = original.branchId,
                warehouseId = original.warehouseId,
                treasuryAccountId = original.treasuryAccountId,
                paymentMethod = original.paymentMethod,
                currencyCode = original.currencyCode,
                exchangeRateAtTx = original.exchangeRateAtTx,
                subtotalOriginal = original.subtotalOriginal,
                discountOriginal = original.discountOriginal,
                taxOriginal = original.taxOriginal,
                totalOriginal = original.totalOriginal,
                subtotalBase = original.subtotalBase,
                discountBase = original.discountBase,
                taxBase = original.taxBase,
                totalBase = original.totalBase,
                paidBase = original.paidBase,
                cogsBase = original.cogsBase,
                journalEntryId = entry.id,
                reversalOfInvoiceId = original.id,
                notes = reason,
            )

            comDao.insertInvoice(returnInvoice)
            comDao.insertInvoiceLines(
                origLines.map { it.copy(id = UUID.randomUUID().toString(), invoiceId = retId) }
            )
            comDao.updateInvoice(
                original.copy(status = "REVERSED", reversedByInvoiceId = retId)
            )

            returnInvoice
        }
    }

    suspend fun postPurchaseInvoice(
        issueDateIso: String,
        partyId: String?,
        branchId: String = "BR-01",
        warehouseId: String = "WH-01",
        treasuryAccountId: String = "TR-CASH-MAIN",
        paymentMethod: String = "CREDIT",
        currencyCode: String = "SDG",
        exchangeRateAtTx: BigDecimal = BigDecimal.ONE,
        lines: List<InvoiceLineRequest>,
        notes: String = "",
        expiryDateIso: String? = null,
        batchNumber: String? = null,
        userId: String = "system",
        userName: String = "System",
    ): InvoiceEntity {
        require(lines.isNotEmpty()) { "Purchase invoice requires at least one line" }
        val profile = db.accountingDao().getProfile()
        val decimals = profile?.decimalPlaces ?: 2

        return db.withTransaction {
            val invNum = doubleEntry.nextSequenceNumber("INV-P")
            val invoiceId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val subOrig = MoneyMath.sumOf(lines.map { MoneyMath.multiply(it.quantity, it.unitPriceOriginal, decimals) })
            var taxOrig = BigDecimal.ZERO
            val lineEntities = lines.map { req ->
                val netOrig = MoneyMath.multiply(req.quantity, req.unitPriceOriginal, decimals)
                val lineTax = MoneyMath.percentage(netOrig, req.taxRatePercent, decimals)
                taxOrig = taxOrig.add(lineTax)
                InvoiceLineEntity(
                    id = UUID.randomUUID().toString(),
                    invoiceId = invoiceId,
                    itemId = req.itemId,
                    itemNameSnapshot = req.itemName,
                    batchId = req.batchId,
                    quantity = req.quantity,
                    unitPriceOriginal = req.unitPriceOriginal,
                    taxRatePercent = req.taxRatePercent,
                    taxAmountOriginal = lineTax,
                    lineTotalOriginal = netOrig.add(lineTax),
                    lineTotalBase = MoneyMath.multiply(netOrig.add(lineTax), exchangeRateAtTx, decimals),
                    unitCostBaseSnapshot = MoneyMath.multiply(req.unitPriceOriginal, exchangeRateAtTx, 4),
                )
            }

            val totalOrig = subOrig.add(taxOrig)
            val subBase = MoneyMath.multiply(subOrig, exchangeRateAtTx, decimals)
            val taxBase = MoneyMath.multiply(taxOrig, exchangeRateAtTx, decimals)
            val totalBase = MoneyMath.multiply(totalOrig, exchangeRateAtTx, decimals)

            val creditAccountId = when (paymentMethod) {
                "CREDIT" -> "2101" // Accounts Payable
                else -> db.commerceDao().getTreasuryById(treasuryAccountId)?.linkedGlAccountId ?: "1101"
            }

            val drafts = mutableListOf<JournalLineDraft>()
            drafts.add(
                JournalLineDraft(
                    accountId = "1104",
                    debitBase = subBase,
                    originalCurrency = currencyCode,
                    originalAmount = subOrig,
                    exchangeRateAtTx = exchangeRateAtTx,
                    branchId = branchId,
                    partyId = partyId,
                    memo = "مشتريات مخزنية $invNum",
                )
            )
            if (taxBase > BigDecimal.ZERO) {
                drafts.add(
                    JournalLineDraft(
                        accountId = "1105",
                        debitBase = taxBase,
                        originalCurrency = currencyCode,
                        originalAmount = taxOrig,
                        exchangeRateAtTx = exchangeRateAtTx,
                        memo = "ضريبة مدخلات مشتريات $invNum",
                    )
                )
            }
            drafts.add(
                JournalLineDraft(
                    accountId = creditAccountId,
                    creditBase = totalBase,
                    originalCurrency = currencyCode,
                    originalAmount = totalOrig,
                    exchangeRateAtTx = exchangeRateAtTx,
                    branchId = branchId,
                    partyId = partyId,
                    memo = "استحقاق/سداد مشتريات $invNum",
                )
            )

            val entry = doubleEntry.postJournalEntry(
                transactionDateIso = issueDateIso,
                description = "قيد فاتورة مشتريات $invNum",
                sourceModule = "PURCHASES",
                sourceDocumentId = invoiceId,
                currencyCode = currencyCode,
                exchangeRateAtTx = exchangeRateAtTx,
                lines = drafts,
                userId = userId,
                userName = userName,
            )

            for (l in lineEntities) {
                if (l.itemId != null) {
                    inventory.receiveStock(
                        itemId = l.itemId,
                        warehouseId = warehouseId,
                        quantity = l.quantity,
                        unitCostBase = l.unitCostBaseSnapshot,
                        sourceRef = invNum,
                        journalEntryId = entry.id,
                        expiryDateIso = expiryDateIso,
                        batchNumber = batchNumber,
                    )
                }
            }

            val invoice = InvoiceEntity(
                id = invoiceId,
                invoiceNumber = invNum,
                invoiceType = "PURCHASE",
                status = "POSTED",
                issueDateIso = issueDateIso,
                recordedAtMillis = now,
                partyId = partyId,
                branchId = branchId,
                warehouseId = warehouseId,
                treasuryAccountId = treasuryAccountId,
                paymentMethod = paymentMethod,
                currencyCode = currencyCode,
                exchangeRateAtTx = MoneyMath.scaleRate(exchangeRateAtTx),
                subtotalOriginal = subOrig,
                discountOriginal = BigDecimal.ZERO,
                taxOriginal = taxOrig,
                totalOriginal = totalOrig,
                subtotalBase = subBase,
                discountBase = BigDecimal.ZERO,
                taxBase = taxBase,
                totalBase = totalBase,
                paidBase = if (paymentMethod == "CREDIT") BigDecimal.ZERO else totalBase,
                journalEntryId = entry.id,
                notes = notes,
            )

            db.commerceDao().insertInvoice(invoice)
            db.commerceDao().insertInvoiceLines(lineEntities)
            invoice
        }
    }

    suspend fun postPurchaseReturn(
        originalPurchaseId: String,
        returnDateIso: String,
        reason: String,
        userId: String = "system",
        userName: String = "System",
    ): InvoiceEntity {
        val comDao = db.commerceDao()
        val original = comDao.getInvoiceById(originalPurchaseId)
            ?: throw IllegalArgumentException("Purchase invoice not found: $originalPurchaseId")
        require(original.invoiceType == "PURCHASE") { "Only PURCHASE invoices can be returned" }
        require(original.status == "POSTED" && original.reversedByInvoiceId == null) {
            "Purchase ${original.invoiceNumber} is already returned"
        }

        val origLines = comDao.getLinesForInvoice(originalPurchaseId)
        return db.withTransaction {
            val retNum = doubleEntry.nextSequenceNumber("RET-P")
            val retId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            for (l in origLines) {
                if (l.itemId != null) {
                    val item = db.inventoryDao().getItemById(l.itemId)
                    if (item != null && !item.isServiceItem) {
                        inventory.consumeStock(
                            itemId = l.itemId,
                            warehouseId = original.warehouseId,
                            quantity = l.quantity,
                            saleDateIso = returnDateIso,
                            sourceRef = retNum,
                        )
                    }
                }
            }

            val debitAccountId = when (original.paymentMethod) {
                "CREDIT" -> "2101"
                else -> comDao.getTreasuryById(original.treasuryAccountId)?.linkedGlAccountId ?: "1101"
            }

            val drafts = mutableListOf<JournalLineDraft>()
            drafts.add(
                JournalLineDraft(
                    accountId = debitAccountId,
                    debitBase = original.totalBase,
                    originalCurrency = original.currencyCode,
                    originalAmount = original.totalOriginal,
                    exchangeRateAtTx = original.exchangeRateAtTx,
                    partyId = original.partyId,
                    memo = "مرتجع مشتريات ${original.invoiceNumber}",
                )
            )
            drafts.add(
                JournalLineDraft(
                    accountId = "1104",
                    creditBase = original.subtotalBase,
                    originalCurrency = original.currencyCode,
                    originalAmount = original.subtotalOriginal,
                    exchangeRateAtTx = original.exchangeRateAtTx,
                    partyId = original.partyId,
                    memo = "رد مخزون مشتريات ${original.invoiceNumber}",
                )
            )
            if (original.taxBase > BigDecimal.ZERO) {
                drafts.add(
                    JournalLineDraft(
                        accountId = "1105",
                        creditBase = original.taxBase,
                        originalCurrency = original.currencyCode,
                        originalAmount = original.taxOriginal,
                        exchangeRateAtTx = original.exchangeRateAtTx,
                        memo = "عكس ضريبة مدخلات ${original.invoiceNumber}",
                    )
                )
            }

            val entry = doubleEntry.postJournalEntry(
                transactionDateIso = returnDateIso,
                description = "قيد مرتجع مشتريات $retNum عن ${original.invoiceNumber} — $reason",
                sourceModule = "PURCHASE_RETURN",
                sourceDocumentId = retId,
                currencyCode = original.currencyCode,
                exchangeRateAtTx = original.exchangeRateAtTx,
                lines = drafts,
                userId = userId,
                userName = userName,
                reversalOfEntryId = original.journalEntryId,
            )

            val retInv = original.copy(
                id = retId,
                invoiceNumber = retNum,
                invoiceType = "PURCHASE_RETURN",
                status = "POSTED",
                issueDateIso = returnDateIso,
                recordedAtMillis = now,
                journalEntryId = entry.id,
                reversalOfInvoiceId = original.id,
                notes = reason,
            )
            comDao.insertInvoice(retInv)
            comDao.insertInvoiceLines(origLines.map { it.copy(id = UUID.randomUUID().toString(), invoiceId = retId) })
            comDao.updateInvoice(original.copy(status = "REVERSED", reversedByInvoiceId = retId))
            retInv
        }
    }

    suspend fun postVoucher(
        voucherType: String, // EXPENSE, INCOME, RECEIPT, PAYMENT, TREASURY_TRANSFER
        dateIso: String,
        debitAccountId: String,
        creditAccountId: String,
        amountOriginal: BigDecimal,
        currencyCode: String = "SDG",
        exchangeRateAtTx: BigDecimal = BigDecimal.ONE,
        description: String,
        partyId: String? = null,
        costCenterId: String? = null,
        userId: String = "system",
        userName: String = "System",
    ): VoucherEntity {
        require(amountOriginal > BigDecimal.ZERO) { "Voucher amount must be positive" }
        val decimals = db.accountingDao().getProfile()?.decimalPlaces ?: 2
        val amountBase = MoneyMath.multiply(amountOriginal, exchangeRateAtTx, decimals)

        return db.withTransaction {
            val voucherNum = doubleEntry.nextSequenceNumber("VCH")
            val entry = doubleEntry.postJournalEntry(
                transactionDateIso = dateIso,
                description = "$voucherType $voucherNum — $description",
                sourceModule = "VOUCHERS",
                currencyCode = currencyCode,
                exchangeRateAtTx = exchangeRateAtTx,
                lines = listOf(
                    JournalLineDraft(
                        accountId = debitAccountId,
                        debitBase = amountBase,
                        originalCurrency = currencyCode,
                        originalAmount = amountOriginal,
                        exchangeRateAtTx = exchangeRateAtTx,
                        partyId = partyId,
                        costCenterId = costCenterId,
                        memo = description,
                    ),
                    JournalLineDraft(
                        accountId = creditAccountId,
                        creditBase = amountBase,
                        originalCurrency = currencyCode,
                        originalAmount = amountOriginal,
                        exchangeRateAtTx = exchangeRateAtTx,
                        partyId = partyId,
                        costCenterId = costCenterId,
                        memo = description,
                    ),
                ),
                userId = userId,
                userName = userName,
            )

            val voucher = VoucherEntity(
                id = UUID.randomUUID().toString(),
                voucherNumber = voucherNum,
                voucherType = voucherType,
                dateIso = dateIso,
                recordedAtMillis = System.currentTimeMillis(),
                fromAccountId = creditAccountId,
                toAccountId = debitAccountId,
                partyId = partyId,
                costCenterId = costCenterId,
                currencyCode = currencyCode,
                exchangeRateAtTx = MoneyMath.scaleRate(exchangeRateAtTx),
                amountOriginal = amountOriginal,
                amountBase = amountBase,
                description = description,
                status = "POSTED",
                journalEntryId = entry.id,
            )
            db.commerceDao().insertVoucher(voucher)
            voucher
        }
    }

    suspend fun postPayrollRun(
        periodLabel: String,
        dateIso: String,
        treasuryAccountId: String = "TR-CASH-MAIN",
        userId: String = "system",
        userName: String = "System",
    ): PayrollRunEntity {
        val employees = db.operationsDao().getActiveEmployees()
        require(employees.isNotEmpty()) { "No active employees found for payroll" }
        val gross = MoneyMath.sumOf(employees.map { it.baseSalaryBase })
        val deductions = MoneyMath.sumOf(employees.map { minOf(it.advanceBalanceBase, it.baseSalaryBase) })
        val net = gross.subtract(deductions)
        val cashGl = db.commerceDao().getTreasuryById(treasuryAccountId)?.linkedGlAccountId ?: "1101"

        return db.withTransaction {
            val drafts = mutableListOf(
                JournalLineDraft(accountId = "5201", debitBase = gross, memo = "رواتب شهر $periodLabel")
            )
            if (deductions > BigDecimal.ZERO) {
                drafts.add(JournalLineDraft(accountId = "1106", creditBase = deductions, memo = "خصم سلف الموظفين"))
            }
            if (net > BigDecimal.ZERO) {
                drafts.add(JournalLineDraft(accountId = cashGl, creditBase = net, memo = "صرف صافي الرواتب $periodLabel"))
            }

            val entry = doubleEntry.postJournalEntry(
                transactionDateIso = dateIso,
                description = "مسير رواتب $periodLabel",
                sourceModule = "PAYROLL",
                lines = drafts,
                userId = userId,
                userName = userName,
            )

            // Zero out deducted advances
            for (emp in employees) {
                val deducted = minOf(emp.advanceBalanceBase, emp.baseSalaryBase)
                if (deducted > BigDecimal.ZERO) {
                    db.operationsDao().upsertEmployee(
                        emp.copy(advanceBalanceBase = emp.advanceBalanceBase.subtract(deducted))
                    )
                }
            }

            val run = PayrollRunEntity(
                id = UUID.randomUUID().toString(),
                periodLabel = periodLabel,
                dateIso = dateIso,
                totalGrossBase = gross,
                totalDeductionsBase = deductions,
                totalNetBase = net,
                treasuryAccountId = treasuryAccountId,
                journalEntryId = entry.id,
                createdAtMillis = System.currentTimeMillis(),
            )
            db.operationsDao().insertPayrollRun(run)
            run
        }
    }
}
