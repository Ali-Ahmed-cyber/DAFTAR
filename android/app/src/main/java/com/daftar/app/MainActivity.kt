package com.daftar.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daftar.app.ui.navigation.DaftarRootNavigation
import com.daftar.app.ui.theme.DaftarTheme
import com.daftar.app.ui.viewmodel.AppViewModel
import com.daftar.app.ui.viewmodel.WorkspaceViewModel

class MainActivity : FragmentActivity() {

    private lateinit var appViewModel: AppViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private var lastBackgroundTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Protect sensitive financial screens from recent-app thumbnails & screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        val app = application as DaftarApplication
        appViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(app, app.container) as T
                }
            }
        )[AppViewModel::class.java]

        workspaceViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkspaceViewModel(app.container.session) as T
                }
            }
        )[WorkspaceViewModel::class.java]

        setContent {
            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
            DaftarTheme(
                language = uiState.language,
                themeMode = uiState.themeMode,
            ) {
                DaftarRootNavigation(
                    appViewModel = appViewModel,
                    workspaceViewModel = workspaceViewModel,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        if (lastBackgroundTimestamp > 0L && elapsed > 30_000L) {
            (application as DaftarApplication).container.lockCurrentSession()
        }
    }
}
