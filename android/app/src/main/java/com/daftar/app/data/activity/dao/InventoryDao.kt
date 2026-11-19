package com.daftar.app.data.activity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daftar.app.data.activity.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM branches ORDER BY isPrimary DESC, code ASC")
    fun observeBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY isPrimary DESC, code ASC")
    suspend fun getBranches(): List<BranchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBranch(branch: BranchEntity)

    @Query("SELECT * FROM warehouses ORDER BY isPrimary DESC, code ASC")
    fun observeWarehouses(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses ORDER BY isPrimary DESC, code ASC")
    suspend fun getWarehouses(): List<WarehouseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWarehouse(warehouse: WarehouseEntity)

    @Query("SELECT * FROM items ORDER BY nameAr ASC")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY nameAr ASC")
    suspend fun getAllItems(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE barcode = :barcode OR sku = :barcode LIMIT 1")
    suspend fun findItemByBarcodeOrSku(barcode: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)

    @Query("SELECT * FROM item_stocks")
    fun observeAllStocks(): Flow<List<ItemStockEntity>>

    @Query("SELECT * FROM item_stocks")
    suspend fun getAllStocks(): List<ItemStockEntity>

    @Query("SELECT * FROM item_stocks WHERE itemId = :itemId AND warehouseId = :warehouseId LIMIT 1")
    suspend fun getStock(itemId: String, warehouseId: String): ItemStockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStock(stock: ItemStockEntity)

    @Query(
        """
        SELECT * FROM inventory_cost_layers
        WHERE itemId = :itemId AND warehouseId = :warehouseId
        ORDER BY receivedAt ASC
        """
    )
    suspend fun getOpenCostLayersFifo(itemId: String, warehouseId: String): List<InventoryCostLayerEntity>

    @Query("SELECT * FROM inventory_cost_layers ORDER BY receivedAt ASC")
    suspend fun getAllCostLayers(): List<InventoryCostLayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostLayer(layer: InventoryCostLayerEntity)

    @Update
    suspend fun updateCostLayer(layer: InventoryCostLayerEntity)

    @Query("SELECT * FROM stock_movements ORDER BY recordedAtMillis DESC")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY recordedAtMillis DESC")
    suspend fun getAllStockMovements(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM pharmacy_batches ORDER BY expiryDateIso ASC")
    fun observePharmacyBatches(): Flow<List<PharmacyBatchEntity>>

    @Query("SELECT * FROM pharmacy_batches ORDER BY expiryDateIso ASC")
    suspend fun getAllPharmacyBatches(): List<PharmacyBatchEntity>

    @Query("SELECT * FROM pharmacy_batches WHERE id = :id LIMIT 1")
    suspend fun getPharmacyBatchById(id: String): PharmacyBatchEntity?

    @Query(
        """
        SELECT * FROM pharmacy_batches
        WHERE itemId = :itemId AND warehouseId = :warehouseId AND isQuarantined = 0
        ORDER BY expiryDateIso ASC
        """
    )
    suspend fun getAvailableBatchesFefo(itemId: String, warehouseId: String): List<PharmacyBatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPharmacyBatch(batch: PharmacyBatchEntity)
}
