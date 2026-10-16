package com.appanuman.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.appanuman.model.AppFilter
import com.appanuman.ui.components.AppBottomBar
import com.appanuman.ui.screens.*
import com.appanuman.viewmodel.MainViewModel

@Composable
fun AppNavGraph(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val userMode by viewModel.userMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val deviceSummary by viewModel.deviceSummary.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val dormantApps by viewModel.dormantApps.collectAsStateWithLifecycle()
    val panicResult by viewModel.panicResult.collectAsStateWithLifecycle()
    val selfAuditProfile by viewModel.selfAuditProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val includeSystemApps by viewModel.includeSystemApps.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        NavRoutes.HOME,
        NavRoutes.APP_LIST,
        NavRoutes.DORMANT_LIST,
        NavRoutes.ABOUT_SCREEN
    ) || (currentRoute?.startsWith("appList") == true)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    userMode = userMode,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavRoutes.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // SCREEN 1: Home
            composable(NavRoutes.HOME) {
                HomeScreen(
                    userMode = userMode,
                    language = language,
                    deviceSummary = deviceSummary,
                    scanState = scanState,
                    onScanNow = { viewModel.startScan() },
                    onNavigateToQuestionList = { filterType ->
                        when (filterType) {
                            "question_otp" -> viewModel.setSelectedFilter(AppFilter.QUESTION_OTP)
                            "question_background" -> viewModel.setSelectedFilter(AppFilter.QUESTION_BACKGROUND)
                            "question_screen" -> viewModel.setSelectedFilter(AppFilter.QUESTION_SCREEN)
                        }
                        navController.navigate(NavRoutes.appListRoute(filterType))
                    },
                    onNavigateToPanic = {
                        navController.navigate(NavRoutes.PANIC_SCREEN)
                    },
                    onSelectMode = { mode -> viewModel.setUserMode(mode) },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }

            // SCREEN 2: App List
            composable(
                route = NavRoutes.APP_LIST,
                arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "all" })
            ) { backStackEntry ->
                val filterArg = backStackEntry.arguments?.getString("filter") ?: "all"
                LaunchedEffect(filterArg) {
                    when (filterArg) {
                        "all" -> viewModel.setSelectedFilter(AppFilter.ALL)
                        "critical" -> viewModel.setSelectedFilter(AppFilter.CRITICAL)
                        "high" -> viewModel.setSelectedFilter(AppFilter.HIGH)
                        "dormant" -> viewModel.setSelectedFilter(AppFilter.DORMANT)
                        "sideloaded" -> viewModel.setSelectedFilter(AppFilter.SIDELOADED)
                        "mismatch" -> viewModel.setSelectedFilter(AppFilter.MISMATCH)
                        "creep" -> viewModel.setSelectedFilter(AppFilter.CREEP)
                        "question_otp" -> viewModel.setSelectedFilter(AppFilter.QUESTION_OTP)
                        "question_background" -> viewModel.setSelectedFilter(AppFilter.QUESTION_BACKGROUND)
                        "question_screen" -> viewModel.setSelectedFilter(AppFilter.QUESTION_SCREEN)
                    }
                }

                AppListScreen(
                    apps = installedApps,
                    userMode = userMode,
                    language = language,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    sortOrder = sortOrder,
                    includeSystemApps = includeSystemApps,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onFilterChange = { viewModel.setSelectedFilter(it) },
                    onSortChange = { viewModel.setSortOrder(it) },
                    onToggleSystemApps = { viewModel.setIncludeSystemApps(it) },
                    onAppClick = { packageName ->
                        navController.navigate(NavRoutes.appDetailRoute(packageName))
                    },
                    onBackClick = { navController.popBackStack() },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }

            // SCREEN 3: App Detail
            composable(
                route = NavRoutes.APP_DETAIL,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val appProfile = viewModel.getApp(packageName)

                AppDetailScreen(
                    app = appProfile,
                    userMode = userMode,
                    language = language,
                    onBackClick = { navController.popBackStack() },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }

            // SCREEN 4: Panic Screen
            composable(NavRoutes.PANIC_SCREEN) {
                PanicScreen(
                    panicResult = panicResult,
                    userMode = userMode,
                    language = language,
                    onRunScan = { viewModel.runPanicAudit() },
                    onAppDetailClick = { packageName ->
                        navController.navigate(NavRoutes.appDetailRoute(packageName))
                    },
                    onBackClick = { navController.popBackStack() },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }

            // SCREEN 5: Dormant Danger Screen
            composable(NavRoutes.DORMANT_LIST) {
                DormantDangerScreen(
                    dormantApps = dormantApps,
                    userMode = userMode,
                    language = language,
                    onAppClick = { packageName ->
                        navController.navigate(NavRoutes.appDetailRoute(packageName))
                    },
                    onBackClick = { navController.popBackStack() },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }

            // SCREEN 6: Share with Family Screen
            composable(NavRoutes.SHARE_SCREEN) {
                ShareFamilyScreen(
                    deviceSummary = deviceSummary,
                    apps = installedApps,
                    userMode = userMode,
                    language = language,
                    onBackClick = { navController.popBackStack() },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }

            // SCREEN 7: About Screen
            composable(NavRoutes.ABOUT_SCREEN) {
                AboutScreen(
                    selfAuditProfile = selfAuditProfile,
                    userMode = userMode,
                    language = language,
                    onBackClick = { navController.popBackStack() },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }
        }
    }
}
