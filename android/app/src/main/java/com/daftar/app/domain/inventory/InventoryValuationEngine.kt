package com.daftar.app.domain.inventory

import com.daftar.app.core.model.InventoryCostingMethod
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.data.activity.entities.*
import com.daftar.app.domain.accounting.DoubleEntryEngine
import com.daftar.app.domain.accounting.JournalLineDraft
import java.math.BigDecimal
import java.util.UUID

class InsufficientStockException(message: String) : IllegalStateException(message)
class ExpiredBatchBlockedException(message: String) : IllegalStateException(message)

class InventoryValuationEngine(
    private val db: ActivityDatabase,
    private val doubleEntry: DoubleEntryEngine,
) {
    suspend fun receiveStock(
        itemId: String,
        warehouseId: String,
        quantity: BigDecimal,
        unitCostBase: BigDecimal,
        sourceRef: String,
        journalEntryId: String? = null,
        batchId: String? = null,
        expiryDateIso: String? = null,
        batchNumber: String? = null,
        manufactureDateIso: String = "",
    ) {
        require(quantity > BigDecimal.ZERO) { "Receive quantity must be positive" }
        require(unitCostBase >= BigDecimal.ZERO) { "Unit cost cannot be negative" }

        val invDao = db.inventoryDao()
        val now = System.currentTimeMillis()
        val totalAddCost = MoneyMath.multiply(quantity, unitCostBase, 4)

        val current = invDao.getStock(itemId, warehouseId)
        val prevQty = current?.quantityOnHand ?: BigDecimal.ZERO
        val prevVal = current?.totalValueBase ?: BigDecimal.ZERO
        val newQty = prevQty.add(quantity)
        val newVal = prevVal.add(totalAddCost)
        val newAvg = if (newQty > BigDecimal.ZERO) MoneyMath.divide(newVal, newQty, 4) else BigDecimal.ZERO

        invDao.upsertStock(
            ItemStockEntity(
                itemId = itemId,
                warehouseId = warehouseId,
                quantityOnHand = newQty,
                avgUnitCostBase = newAvg,
                totalValueBase = newVal,
            )
        )

        var resolvedBatchId = batchId
        if (!expiryDateIso.isNullOrBlank() && !batchNumber.isNullOrBlank()) {
            val newBatchId = batchId ?: UUID.randomUUID().toString()
            resolvedBatchId = newBatchId
            invDao.upsertPharmacyBatch(
                PharmacyBatchEntity(
                    id = newBatchId,
                    itemId = itemId,
                    warehouseId = warehouseId,
                    batchNumber = batchNumber,
                    manufactureDateIso = manufactureDateIso,
                    expiryDateIso = expiryDateIso,
                    quantityRemaining = quantity,
                    unitCostBase = unitCostBase,
                )
            )
        }

        invDao.insertCostLayer(
            InventoryCostLayerEntity(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                warehouseId = warehouseId,
                batchId = resolvedBatchId,
                sourceRef = sourceRef,
                receivedAt = now,
                initialQty = quantity,
                remainingQty = quantity,
                unitCostBase = unitCostBase,
            )
        )

        invDao.insertStockMovement(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                warehouseId = warehouseId,
                movementType = "IN",
                quantityDelta = quantity,
                unitCostBase = unitCostBase,
                totalCostBase = totalAddCost,
                sourceRef = sourceRef,
                journalEntryId = journalEntryId,
                recordedAtMillis = now,
            )
        )
    }

    suspend fun consumeStock(
        itemId: String,
        warehouseId: String,
        quantity: BigDecimal,
        saleDateIso: String,
        sourceRef: String,
        explicitBatchId: String? = null,
        journalEntryId: String? = null,
    ): BigDecimal {
        require(quantity > BigDecimal.ZERO) { "Consume quantity must be positive" }

        val invDao = db.inventoryDao()
        val item = invDao.getItemById(itemId)
            ?: throw IllegalArgumentException("Unknown item: $itemId")

        if (item.isServiceItem) return BigDecimal.ZERO

        val stock = invDao.getStock(itemId, warehouseId)
        val onHand = stock?.quantityOnHand ?: BigDecimal.ZERO
        if (onHand < quantity) {
            throw InsufficientStockException(
                "Insufficient stock for ${item.nameEn}: available=$onHand, requested=$quantity"
            )
        }

        // Pharmacy FEFO & hard block for expired batches
        if (item.trackExpiryBatches || explicitBatchId != null) {
            if (explicitBatchId != null) {
                val batch = invDao.getPharmacyBatchById(explicitBatchId)
                    ?: throw IllegalArgumentException("Batch not found: $explicitBatchId")
                if (batch.isQuarantined) {
                    throw ExpiredBatchBlockedException("Batch ${batch.batchNumber} is quarantined")
                }
                if (batch.expiryDateIso < saleDateIso) {
                    throw ExpiredBatchBlockedException(
                        "Cannot dispense expired batch ${batch.batchNumber} (expired ${batch.expiryDateIso} < $saleDateIso)"
                    )
                }
                if (batch.quantityRemaining < quantity) {
                    throw InsufficientStockException("Batch ${batch.batchNumber} has only ${batch.quantityRemaining} units")
                }
                invDao.upsertPharmacyBatch(
                    batch.copy(quantityRemaining = batch.quantityRemaining.subtract(quantity))
                )
            } else {
                val batches = invDao.getAvailableBatchesFefo(itemId, warehouseId)
                if (batches.isNotEmpty()) {
                    val validBatches = batches.filter { it.expiryDateIso >= saleDateIso && it.quantityRemaining > BigDecimal.ZERO }
                    val totalValidQty = MoneyMath.sumOf(validBatches.map { it.quantityRemaining })
                    if (totalValidQty < quantity) {
                        throw ExpiredBatchBlockedException(
                            "No non-expired stock available for ${item.nameEn} on $saleDateIso"
                        )
                    }
                    var remainingToDeduct = quantity
                    for (batch in validBatches) {
                        if (remainingToDeduct <= BigDecimal.ZERO) break
                        val take = minOf(batch.quantityRemaining, remainingToDeduct)
                        invDao.upsertPharmacyBatch(
                            batch.copy(quantityRemaining = batch.quantityRemaining.subtract(take))
                        )
                        remainingToDeduct = remainingToDeduct.subtract(take)
                    }
                }
            }
        }

        val profile = db.accountingDao().getProfile()
        val costingMethod = InventoryCostingMethod.fromId(profile?.costingMethodId ?: "wac")
        val now = System.currentTimeMillis()

        // Filter open cost layers strictly in Kotlin using BigDecimal comparison
        val openLayers = invDao.getOpenCostLayersFifo(itemId, warehouseId)
            .filter { it.remainingQty > BigDecimal.ZERO }

        val cogsBase: BigDecimal = when (costingMethod) {
            InventoryCostingMethod.FIFO -> {
                var toConsume = quantity
                var accumulatedCost = BigDecimal.ZERO
                for (layer in openLayers) {
                    if (toConsume <= BigDecimal.ZERO) break
                    val take = minOf(layer.remainingQty, toConsume)
                    accumulatedCost = accumulatedCost.add(MoneyMath.multiply(take, layer.unitCostBase, 4))
                    invDao.updateCostLayer(
                        layer.copy(remainingQty = layer.remainingQty.subtract(take))
                    )
                    toConsume = toConsume.subtract(take)
                }
                if (toConsume > BigDecimal.ZERO) {
                    accumulatedCost = accumulatedCost.add(
                        MoneyMath.multiply(toConsume, stock?.avgUnitCostBase ?: item.defaultPurchaseCostBase, 4)
                    )
                }
                MoneyMath.scaleMoney(accumulatedCost, profile?.decimalPlaces ?: 2)
            }
            InventoryCostingMethod.WEIGHTED_AVERAGE -> {
                val avg = stock?.avgUnitCostBase ?: item.defaultPurchaseCostBase
                // Also proportionally keep FIFO layers drained so switching methods remains coherent
                var toDrain = quantity
                for (layer in openLayers) {
                    if (toDrain <= BigDecimal.ZERO) break
                    val take = minOf(layer.remainingQty, toDrain)
                    invDao.updateCostLayer(layer.copy(remainingQty = layer.remainingQty.subtract(take)))
                    toDrain = toDrain.subtract(take)
                }
                MoneyMath.multiply(quantity, avg, profile?.decimalPlaces ?: 2)
            }
        }

        val newQty = onHand.subtract(quantity)
        val prevVal = stock?.totalValueBase ?: BigDecimal.ZERO
        val newVal = (prevVal.subtract(cogsBase)).max(BigDecimal.ZERO)
        val newAvg = if (newQty > BigDecimal.ZERO) MoneyMath.divide(newVal, newQty, 4) else BigDecimal.ZERO

        invDao.upsertStock(
            ItemStockEntity(
                itemId = itemId,
                warehouseId = warehouseId,
                quantityOnHand = newQty,
                avgUnitCostBase = newAvg,
                totalValueBase = newVal,
            )
        )

        val unitCogs = if (quantity > BigDecimal.ZERO) MoneyMath.divide(cogsBase, quantity, 4) else BigDecimal.ZERO
        invDao.insertStockMovement(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                warehouseId = warehouseId,
                movementType = "OUT",
                quantityDelta = quantity.negate(),
                unitCostBase = unitCogs,
                totalCostBase = cogsBase,
                sourceRef = sourceRef,
                journalEntryId = journalEntryId,
                recordedAtMillis = now,
            )
        )

        return cogsBase
    }

    suspend fun transferBetweenWarehouses(
        itemId: String,
        fromWarehouseId: String,
        toWarehouseId: String,
        quantity: BigDecimal,
        dateIso: String,
        reference: String,
    ) {
        require(fromWarehouseId != toWarehouseId) { "Source and destination warehouses must differ" }
        val totalCost = consumeStock(itemId, fromWarehouseId, quantity, dateIso, "XFER-OUT-$reference")
        val unitCost = MoneyMath.divide(totalCost, quantity, 4)
        receiveStock(itemId, toWarehouseId, quantity, unitCost, "XFER-IN-$reference")
    }

    suspend fun recordStockAdjustment(
        itemId: String,
        warehouseId: String,
        deltaQty: BigDecimal,
        dateIso: String,
        reason: String,
        userId: String = "system",
        userName: String = "System",
    ) {
        require(deltaQty.compareTo(BigDecimal.ZERO) != 0) { "Adjustment delta cannot be zero" }
        val invDao = db.inventoryDao()
        val item = invDao.getItemById(itemId) ?: throw IllegalArgumentException("Item not found")
        val stock = invDao.getStock(itemId, warehouseId)
        val unitCost = stock?.avgUnitCostBase?.takeIf { it > BigDecimal.ZERO } ?: item.defaultPurchaseCostBase

        if (deltaQty > BigDecimal.ZERO) {
            val totalCost = MoneyMath.multiply(deltaQty, unitCost, 2)
            val entry = doubleEntry.postJournalEntry(
                transactionDateIso = dateIso,
                description = "تسوية جردية بالزيادة: ${item.nameAr} — $reason",
                sourceModule = "INVENTORY_ADJUSTMENT",
                lines = listOf(
                    JournalLineDraft(accountId = "1104", debitBase = totalCost, memo = reason),
                    JournalLineDraft(accountId = "5102", creditBase = totalCost, memo = reason),
                ),
                userId = userId,
                userName = userName,
            )
            receiveStock(itemId, warehouseId, deltaQty, unitCost, "ADJ-$reason", entry.id)
        } else {
            val qtyOut = deltaQty.abs()
            val cogs = consumeStock(itemId, warehouseId, qtyOut, dateIso, "ADJ-$reason")
            doubleEntry.postJournalEntry(
                transactionDateIso = dateIso,
                description = "تسوية جردية بالعجز: ${item.nameAr} — $reason",
                sourceModule = "INVENTORY_ADJUSTMENT",
                lines = listOf(
                    JournalLineDraft(accountId = "5102", debitBase = cogs, memo = reason),
                    JournalLineDraft(accountId = "1104", creditBase = cogs, memo = reason),
                ),
                userId = userId,
                userName = userName,
            )
        }
    }
}
