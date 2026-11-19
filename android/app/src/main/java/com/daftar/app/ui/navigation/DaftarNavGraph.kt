package com.daftar.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.daftar.app.core.model.AppLanguage
import com.daftar.app.core.model.ScreenRoute
import com.daftar.app.ui.components.DaftarTopBar
import com.daftar.app.ui.screens.*
import com.daftar.app.ui.viewmodel.AppViewModel
import com.daftar.app.ui.viewmodel.WorkspaceSnapshot
import com.daftar.app.ui.viewmodel.WorkspaceViewModel

@Composable
private fun WithWorkspaceSnapshot(
    workspaceViewModel: WorkspaceViewModel,
    content: @Composable (WorkspaceSnapshot) -> Unit,
) {
    val snap by workspaceViewModel.snapshot.collectAsStateWithLifecycle()
    content(snap)
}

@Composable
fun DaftarRootNavigation(
    appViewModel: AppViewModel,
    workspaceViewModel: WorkspaceViewModel,
) {
    val navController = rememberNavController()
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val lang = uiState.language

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = backStackEntry?.destination?.route ?: ScreenRoute.SPLASH.route
    val currentScreen = ScreenRoute.entries.firstOrNull { it.route == currentRouteName } ?: ScreenRoute.SPLASH

    // Automatically return to LOGIN when vault session is locked (e.g., on background timeout)
    LaunchedEffect(session?.authenticatedUser, currentScreen) {
        if (currentScreen.showBottomNav && session?.authenticatedUser == null) {
            navController.navigate(ScreenRoute.LOGIN.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    fun navigateTo(screen: ScreenRoute) {
        navController.navigate(screen.route) {
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = {
            if (currentScreen.showBottomNav) {
                DaftarTopBar(
                    title = currentScreen.title.get(lang),
                    subtitle = currentScreen.subtitle.get(lang),
                    canNavigateBack = currentScreen != ScreenRoute.DASHBOARD,
                    onBack = { navController.popBackStack() },
                    actions = {
                        IconButton(onClick = { navigateTo(ScreenRoute.NOTIFICATIONS) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                        }
                        IconButton(onClick = { navigateTo(ScreenRoute.SETTINGS) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen.showBottomNav) {
                NavigationBar {
                    listOf(
                        ScreenRoute.DASHBOARD to Icons.Default.Dashboard,
                        ScreenRoute.JOURNAL_ENTRIES to Icons.Default.Book,
                        ScreenRoute.SALES to Icons.Default.ReceiptLong,
                        ScreenRoute.INVENTORY to Icons.Default.Inventory2,
                        ScreenRoute.MAIN_MENU to Icons.Default.Apps,
                    ).forEach { (route, icon) ->
                        NavigationBarItem(
                            selected = currentScreen == route,
                            onClick = { navigateTo(route) },
                            icon = { Icon(icon, contentDescription = null) },
                            label = {
                                val short = if (lang == AppLanguage.ARABIC) {
                                    when (route) {
                                        ScreenRoute.DASHBOARD -> "الرئيسية"
                                        ScreenRoute.JOURNAL_ENTRIES -> "القيود"
                                        ScreenRoute.SALES -> "المبيعات"
                                        ScreenRoute.INVENTORY -> "المخزون"
                                        else -> "الوحدات"
                                    }
                                } else {
                                    when (route) {
                                        ScreenRoute.DASHBOARD -> "Home"
                                        ScreenRoute.JOURNAL_ENTRIES -> "Ledger"
                                        ScreenRoute.SALES -> "Sales"
                                        ScreenRoute.INVENTORY -> "Stock"
                                        else -> "Modules"
                                    }
                                }
                                Text(short)
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = ScreenRoute.SPLASH.route,
            ) {
                composable(ScreenRoute.SPLASH.route) {
                    SplashScreen(
                        uiState = uiState,
                        onNavigateToLogin = {
                            navController.navigate(ScreenRoute.LOGIN.route) {
                                popUpTo(ScreenRoute.SPLASH.route) { inclusive = true }
                            }
                        },
                        onNavigateToBusinessType = {
                            navController.navigate(ScreenRoute.BUSINESS_TYPE.route) {
                                popUpTo(ScreenRoute.SPLASH.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(ScreenRoute.BUSINESS_TYPE.route) {
                    BusinessTypeScreen(
                        appViewModel = appViewModel,
                        uiState = uiState,
                        onContinue = { navigateTo(ScreenRoute.CREATE_ACTIVITY) },
                        onRestoreBackup = { navigateTo(ScreenRoute.RESTORE) },
                    )
                }

                composable(ScreenRoute.CREATE_ACTIVITY.route) {
                    CreateActivityScreen(
                        appViewModel = appViewModel,
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onCreated = {
                            navController.navigate(ScreenRoute.DASHBOARD.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                composable(ScreenRoute.LOGIN.route) {
                    LoginScreen(
                        appViewModel = appViewModel,
                        uiState = uiState,
                        onCreateNewActivity = { navigateTo(ScreenRoute.BUSINESS_TYPE) },
                        onRestoreBackup = { navigateTo(ScreenRoute.RESTORE) },
                        onAuthenticated = {
                            navController.navigate(ScreenRoute.DASHBOARD.route) {
                                popUpTo(ScreenRoute.LOGIN.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(ScreenRoute.RESTORE.route) {
                    RestoreScreen(
                        appViewModel = appViewModel,
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onRestored = {
                            navController.navigate(ScreenRoute.LOGIN.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                composable(ScreenRoute.DASHBOARD.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        DashboardScreen(
                            appViewModel = appViewModel,
                            uiState = uiState,
                            snapshot = snap,
                            onNavigate = ::navigateTo,
                            onSwitchVault = {
                                appViewModel.container.lockCurrentSession()
                                navController.navigate(ScreenRoute.LOGIN.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }

                composable(ScreenRoute.MAIN_MENU.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        MainMenuScreen(lang, snap, ::navigateTo)
                    }
                }

                composable(ScreenRoute.NOTIFICATIONS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        NotificationsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.ACCOUNTS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        ChartOfAccountsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.JOURNAL_ENTRIES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        JournalEntriesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.GENERAL_LEDGER.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        GeneralLedgerScreen(lang, snap)
                    }
                }

                composable(ScreenRoute.TREASURY_BANKS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        TreasuryBanksScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.EXPENSES_REVENUES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        ExpensesRevenuesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.FIXED_ASSETS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        FixedAssetsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.TRIAL_BALANCE.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        TrialBalanceScreen(lang, snap)
                    }
                }

                composable(ScreenRoute.FINANCIAL_STATEMENTS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        FinancialStatementsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.CASH_FLOWS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        CashFlowsScreen(lang, snap)
                    }
                }

                composable(ScreenRoute.SALES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        SalesScreen(
                            lang = lang,
                            vm = workspaceViewModel,
                            snapshot = snap,
                            onCreateInvoice = { navigateTo(ScreenRoute.CREATE_INVOICE) },
                            onOpenPos = { navigateTo(ScreenRoute.POS_TERMINAL) },
                        )
                    }
                }

                composable(ScreenRoute.CREATE_INVOICE.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        CreateInvoiceScreen(lang, workspaceViewModel, snap) { navController.popBackStack() }
                    }
                }

                composable(ScreenRoute.POS_TERMINAL.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        PosTerminalScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.PURCHASES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        PurchasesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.PARTIES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        PartiesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.INVENTORY.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        InventoryScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.BARCODE_QR.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        BarcodeQrScreen(lang, snap)
                    }
                }

                composable(ScreenRoute.EMPLOYEES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        EmployeesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.BUDGETS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        BudgetsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.ATTACHMENTS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        AttachmentsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.BRANCHES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        BranchesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.PROJECTS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        ProjectsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.MANUFACTURING.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        ManufacturingScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.RESTAURANTS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        RestaurantsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.PHARMACIES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        PharmaciesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.WORKSHOPS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        WorkshopsScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.SHIPPING.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        ShippingScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.LOYALTY.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        LoyaltyScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.TAXES_CURRENCIES.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        TaxesCurrenciesScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.REPORTS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        ReportsAndAuditScreen(lang, snap)
                    }
                }

                composable(ScreenRoute.AI_ASSISTANT.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        SmartAssistantScreen(lang, workspaceViewModel, snap)
                    }
                }

                composable(ScreenRoute.BACKUP.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        BackupScreen(
                            appViewModel = appViewModel,
                            uiState = uiState,
                            snapshot = snap,
                            onOpenRestore = { navigateTo(ScreenRoute.RESTORE) },
                        )
                    }
                }

                composable(ScreenRoute.SECURITY.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        SecurityScreen(lang, appViewModel, snap)
                    }
                }

                composable(ScreenRoute.SETTINGS.route) {
                    WithWorkspaceSnapshot(workspaceViewModel) { snap ->
                        SettingsAndUsersScreen(
                            appViewModel = appViewModel,
                            uiState = uiState,
                            vm = workspaceViewModel,
                            snapshot = snap,
                            onSwitchVault = {
                                appViewModel.container.lockCurrentSession()
                                navController.navigate(ScreenRoute.LOGIN.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
