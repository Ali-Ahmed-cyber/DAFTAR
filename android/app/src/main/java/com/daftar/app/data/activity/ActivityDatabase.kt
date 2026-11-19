package com.daftar.app.data.activity

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.daftar.app.data.activity.dao.*
import com.daftar.app.data.activity.entities.*
import com.daftar.app.data.converter.RoomTypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ActivityProfileEntity::class,
        CurrencyEntity::class,
        TaxRuleEntity::class,
        FiscalPeriodEntity::class,
        CostCenterEntity::class,
        AccountEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        PartyEntity::class,
        InvoiceEntity::class,
        InvoiceLineEntity::class,
        VoucherEntity::class,
        TreasuryAccountEntity::class,
        BankReconciliationEntity::class,
        BranchEntity::class,
        WarehouseEntity::class,
        ItemEntity::class,
        ItemStockEntity::class,
        InventoryCostLayerEntity::class,
        StockMovementEntity::class,
        PharmacyBatchEntity::class,
        EmployeeEntity::class,
        PayrollRunEntity::class,
        FixedAssetEntity::class,
        BudgetEntity::class,
        AttachmentEntity::class,
        ProjectEntity::class,
        ManufacturingBomEntity::class,
        ProductionOrderEntity::class,
        RestaurantTableEntity::class,
        KitchenOrderEntity::class,
        WorkshopJobEntity::class,
        ShippingOrderEntity::class,
        LoyaltyOfferEntity::class,
        PartnerStakeEntity::class,
        UserEntity::class,
        AuditLogEntity::class,
        LocalNotificationEntity::class,
        SequenceCounterEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun accountingDao(): AccountingDao
    abstract fun commerceDao(): CommerceDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun operationsDao(): OperationsDao
    abstract fun specializedDao(): SpecializedDao
    abstract fun governanceDao(): GovernanceDao

    companion object {
        fun openEncrypted(
            context: Context,
            dbFileName: String,
            passphrase: ByteArray,
        ): ActivityDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                ActivityDatabase::class.java,
                dbFileName,
            )
                .openHelperFactory(factory)
                .build()
        }

        fun createInMemoryForTest(context: Context): ActivityDatabase =
            Room.inMemoryDatabaseBuilder(context, ActivityDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
