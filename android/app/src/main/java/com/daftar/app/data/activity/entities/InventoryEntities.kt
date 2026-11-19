package com.daftar.app.data.activity.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val city: String = "الخرطوم",
    val costCenterId: String? = null,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
)

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val branchId: String,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "items",
    indices = [Index(value = ["sku"], unique = true), Index(value = ["barcode"])]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val barcode: String,
    val nameAr: String,
    val nameEn: String,
    val categoryName: String,
    val unitName: String = "PCS",
    val salePriceBase: BigDecimal,
    val defaultPurchaseCostBase: BigDecimal,
    val taxRuleId: String? = null,
    val reorderLevel: BigDecimal = BigDecimal("5"),
    val trackExpiryBatches: Boolean = false,
    val isRawMaterial: Boolean = false,
    val isServiceItem: Boolean = false,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "item_stocks",
    primaryKeys = ["itemId", "warehouseId"]
)
data class ItemStockEntity(
    val itemId: String,
    val warehouseId: String,
    val quantityOnHand: BigDecimal,
    val avgUnitCostBase: BigDecimal,
    val totalValueBase: BigDecimal,
)

@Entity(
    tableName = "inventory_cost_layers",
    indices = [Index("itemId"), Index("warehouseId"), Index("receivedAt")]
)
data class InventoryCostLayerEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val warehouseId: String,
    val batchId: String? = null,
    val sourceRef: String,
    val receivedAt: Long,
    val initialQty: BigDecimal,
    val remainingQty: BigDecimal,
    val unitCostBase: BigDecimal,
)

@Entity(
    tableName = "stock_movements",
    indices = [Index("itemId"), Index("warehouseId")]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val warehouseId: String,
    val movementType: String, // IN, OUT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT, PRODUCTION_IN, PRODUCTION_OUT
    val quantityDelta: BigDecimal,
    val unitCostBase: BigDecimal,
    val totalCostBase: BigDecimal,
    val sourceRef: String,
    val journalEntryId: String? = null,
    val recordedAtMillis: Long,
)

@Entity(
    tableName = "pharmacy_batches",
    indices = [Index("itemId"), Index("expiryDateIso")]
)
data class PharmacyBatchEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val warehouseId: String,
    val batchNumber: String,
    val manufactureDateIso: String,
    val expiryDateIso: String,
    val quantityRemaining: BigDecimal,
    val unitCostBase: BigDecimal,
    val isQuarantined: Boolean = false,
)
