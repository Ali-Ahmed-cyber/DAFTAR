package com.daftar.app.data.activity.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daftar.app.data.activity.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecializedDao {
    @Query("SELECT * FROM projects ORDER BY startDateIso DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY startDateIso DESC")
    suspend fun getProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("SELECT * FROM manufacturing_boms ORDER BY nameAr ASC")
    fun observeBoms(): Flow<List<ManufacturingBomEntity>>

    @Query("SELECT * FROM manufacturing_boms")
    suspend fun getBoms(): List<ManufacturingBomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBom(bom: ManufacturingBomEntity)

    @Query("SELECT * FROM production_orders ORDER BY dateIso DESC")
    fun observeProductionOrders(): Flow<List<ProductionOrderEntity>>

    @Query("SELECT * FROM production_orders ORDER BY dateIso DESC")
    suspend fun getProductionOrders(): List<ProductionOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductionOrder(order: ProductionOrderEntity)

    @Query("SELECT * FROM restaurant_tables ORDER BY tableNumber ASC")
    fun observeRestaurantTables(): Flow<List<RestaurantTableEntity>>

    @Query("SELECT * FROM restaurant_tables ORDER BY tableNumber ASC")
    suspend fun getRestaurantTables(): List<RestaurantTableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestaurantTable(table: RestaurantTableEntity)

    @Query("SELECT * FROM kitchen_orders ORDER BY createdAtMillis DESC")
    fun observeKitchenOrders(): Flow<List<KitchenOrderEntity>>

    @Query("SELECT * FROM kitchen_orders ORDER BY createdAtMillis DESC")
    suspend fun getKitchenOrders(): List<KitchenOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKitchenOrder(order: KitchenOrderEntity)

    @Query("SELECT * FROM workshop_jobs ORDER BY createdDateIso DESC")
    fun observeWorkshopJobs(): Flow<List<WorkshopJobEntity>>

    @Query("SELECT * FROM workshop_jobs ORDER BY createdDateIso DESC")
    suspend fun getWorkshopJobs(): List<WorkshopJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkshopJob(job: WorkshopJobEntity)

    @Query("SELECT * FROM shipping_orders ORDER BY updatedAtMillis DESC")
    fun observeShippingOrders(): Flow<List<ShippingOrderEntity>>

    @Query("SELECT * FROM shipping_orders ORDER BY updatedAtMillis DESC")
    suspend fun getShippingOrders(): List<ShippingOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShippingOrder(order: ShippingOrderEntity)

    @Query("SELECT * FROM loyalty_offers")
    fun observeLoyaltyOffers(): Flow<List<LoyaltyOfferEntity>>

    @Query("SELECT * FROM loyalty_offers")
    suspend fun getLoyaltyOffers(): List<LoyaltyOfferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoyaltyOffer(offer: LoyaltyOfferEntity)

    @Query("SELECT * FROM partner_stakes ORDER BY sharePercent DESC")
    fun observePartnerStakes(): Flow<List<PartnerStakeEntity>>

    @Query("SELECT * FROM partner_stakes ORDER BY sharePercent DESC")
    suspend fun getPartnerStakes(): List<PartnerStakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPartnerStake(stake: PartnerStakeEntity)
}
