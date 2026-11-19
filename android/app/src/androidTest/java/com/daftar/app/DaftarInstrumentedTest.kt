package com.daftar.app

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daftar.app.core.model.BusinessType
import com.daftar.app.core.security.VaultKeyProvider
import com.daftar.app.data.activity.ActivityDatabase
import com.daftar.app.domain.accounting.StarterChartOfAccounts
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaftarInstrumentedTest {

    @Test
    fun manifestDeclaresZeroInternetPermission() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pkgInfo = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val perms = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse("Manifest must NOT request INTERNET", perms.contains(android.Manifest.permission.INTERNET))
    }

    @Test
    fun sqlCipherEncryptedActivityDatabaseOpensAndPersists() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val key = ByteArray(32) { (it + 1).toByte() }
        val db = ActivityDatabase.openEncrypted(ctx, "daftar_instr_test.db", key)
        try {
            StarterChartOfAccounts.initializeNewActivityDatabase(
                db = db,
                vaultId = "instr-1",
                nameAr = "اختبار مشفر",
                nameEn = "Encrypted Test",
                businessType = BusinessType.PHARMACY,
                adminFullName = "Admin",
                adminPin = "9513",
            )
            assertEquals("SDG", db.accountingDao().getProfile()!!.baseCurrency)
        } finally {
            db.close()
            ctx.deleteDatabase("daftar_instr_test.db")
        }
    }
}
