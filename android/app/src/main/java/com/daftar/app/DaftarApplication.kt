package com.daftar.app

import android.app.Application
import com.daftar.app.core.backup.LocalMaintenanceWorker
import com.daftar.app.core.di.DaftarAppContainer

class DaftarApplication : Application() {
    val container: DaftarAppContainer by lazy {
        DaftarAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        LocalMaintenanceWorker.schedulePeriodicChecks(this)
    }
}
