package com.daftar.app.data.activity.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val partyId: String? = null,
    val costCenterId: String,
    val contractValueBase: BigDecimal,
    val status: String = "ACTIVE", // ACTIVE, ON_HOLD, COMPLETED
    val startDateIso: String,
)

@Entity(tableName = "manufacturing_boms")
data class ManufacturingBomEntity(
    @PrimaryKey val id: String,
    val finishedItemId: String,
    val nameAr: String,
    val nameEn: String,
    val outputQty: BigDecimal,
    val componentsJson: String, // itemId:qty pairs
    val estimatedLaborCostBase: BigDecimal = BigDecimal.ZERO,
)

@Entity(tableName = "production_orders")
data class ProductionOrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val bomId: String,
    val finishedItemId: String,
    val quantityPlanned: BigDecimal,
    val quantityProduced: BigDecimal,
    val warehouseId: String,
    val totalCostBase: BigDecimal,
    val status: String, // PLANNED, COMPLETED
    val journalEntryId: String? = null,
    val dateIso: String,
)

@Entity(tableName = "restaurant_tables")
data class RestaurantTableEntity(
    @PrimaryKey val id: String,
    val tableNumber: String,
    val zoneName: String,
    val capacity: Int,
    val status: String = "AVAILABLE", // AVAILABLE, OCCUPIED, RESERVED
    val activeOrderId: String? = null,
)

@Entity(tableName = "kitchen_orders")
data class KitchenOrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val tableId: String?,
    val orderType: String, // DINE_IN, TAKEAWAY, DELIVERY
    val itemsSummary: String,
    val totalOriginal: BigDecimal,
    val status: String, // NEW, PREPARING, READY, SERVED, INVOICED
    val linkedInvoiceId: String? = null,
    val createdAtMillis: Long,
)

@Entity(tableName = "workshop_jobs")
data class WorkshopJobEntity(
    @PrimaryKey val id: String,
    val jobNumber: String,
    val customerId: String?,
    val assetDescription: String, // Vehicle / Device
    val issueSummary: String,
    val sparePartsCostBase: BigDecimal = BigDecimal.ZERO,
    val laborFeeBase: BigDecimal = BigDecimal.ZERO,
    val status: String, // INTAKE, DIAGNOSIS, IN_REPAIR, READY, DELIVERED
    val linkedInvoiceId: String? = null,
    val createdDateIso: String,
)

@Entity(tableName = "shipping_orders")
data class ShippingOrderEntity(
    @PrimaryKey val id: String,
    val waybillNumber: String,
    val invoiceId: String?,
    val recipientName: String,
    val destinationCity: String,
    val carrierName: String,
    val codAmountBase: BigDecimal,
    val shippingFeeBase: BigDecimal,
    val status: String, // PENDING, IN_TRANSIT, DELIVERED, COD_SETTLED
    val updatedAtMillis: Long,
)

@Entity(tableName = "loyalty_offers")
data class LoyaltyOfferEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val discountPercent: BigDecimal,
    val pointsPerThousandBase: Int,
    val isActive: Boolean = true,
)

@Entity(tableName = "partner_stakes")
data class PartnerStakeEntity(
    @PrimaryKey val id: String,
    val partnerName: String,
    val sharePercent: BigDecimal,
    val contributedCapitalBase: BigDecimal,
    val capitalAccountId: String = "3101",
    val drawingAccountId: String = "3102",
)
