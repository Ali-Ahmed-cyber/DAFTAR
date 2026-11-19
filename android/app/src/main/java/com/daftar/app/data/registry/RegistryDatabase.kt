package com.daftar.app.data.registry

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ActivityVaultEntity::class,
        AppPreferencesEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RegistryDatabase : RoomDatabase() {
    abstract fun registryDao(): RegistryDao

    companion object {
        private const val REGISTRY_DB_NAME = "daftar_registry_encrypted.db"

        fun create(context: Context, passphrase: ByteArray): RegistryDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                RegistryDatabase::class.java,
                REGISTRY_DB_NAME,
            )
                .openHelperFactory(factory)
                .build()
        }

        fun createInMemoryForTest(context: Context): RegistryDatabase =
            Room.inMemoryDatabaseBuilder(context, RegistryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
