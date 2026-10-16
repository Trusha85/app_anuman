package com.appanuman.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appanuman.data.model.Language
import com.appanuman.features.dormant.DormantDangerScreen
import com.appanuman.features.panic.PanicScreen
import com.appanuman.features.privacy.PrivacyModeScreen
import com.appanuman.features.share.ShareFamilyScreen
import com.appanuman.ui.MainViewModel
import com.appanuman.ui.screens.AppDetailScreen
import com.appanuman.ui.screens.AppListScreen
import com.appanuman.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val APP_LIST = "appList?filterType={filterType}"
    const val APP_DETAIL = "appDetail/{packageName}"
    const val PANIC_SCREEN = "panicScreen"
    const val DORMANT_LIST = "dormantList"
    const val SHARE_SCREEN = "shareScreen"
    const val PRIVACY_SCREEN = "privacyScreen"

    fun appListWithFilter(filter: String) = "appList?filterType=$filter"
    fun appDetail(pkg: String) = "appDetail/$pkg"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    initialPackageName: String? = null,
    navController: NavHostController = rememberNavController()
) {
    val apps by viewModel.appList.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanDuration by viewModel.scanDuration.collectAsState()
    val deviceGrade by viewModel.deviceRiskGrade.collectAsState()
    val deviceScore by viewModel.deviceRiskScore.collectAsState()
    val userMode by viewModel.userMode.collectAsState()
    val language by viewModel.language.collectAsState()

    val context = LocalContext.current
    val selfProfile = remember(apps) { apps.find { it.packageName == context.packageName } }

    LaunchedEffect(initialPackageName) {
        if (!initialPackageName.isNullOrEmpty()) {
            navController.navigate(Routes.appDetail(initialPackageName))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                apps = apps,
                isScanning = isScanning,
                scanDuration = scanDuration,
                deviceGrade = deviceGrade,
                deviceScore = deviceScore,
                userMode = userMode,
                language = language,
                onScanClick = { viewModel.scanApps() },
                onQuestionClick = { filter ->
                    navController.navigate(Routes.appListWithFilter(filter))
                },
                onPanicClick = {
                    navController.navigate(Routes.PANIC_SCREEN)
                },
                onDormantClick = {
                    navController.navigate(Routes.DORMANT_LIST)
                },
                onShareClick = {
                    navController.navigate(Routes.SHARE_SCREEN)
                },
                onPrivacyClick = {
                    navController.navigate(Routes.PRIVACY_SCREEN)
                },
                onAllAppsClick = {
                    navController.navigate(Routes.appListWithFilter("ALL"))
                },
                onLanguageToggle = {
                    val nextLang = if (language == Language.EN) Language.HI else Language.EN
                    viewModel.setLanguage(nextLang)
                },
                onModeChange = { mode ->
                    viewModel.setUserMode(mode)
                }
            )
        }

        composable(
            route = Routes.APP_LIST,
            arguments = listOf(
                navArgument("filterType") {
                    type = NavType.StringType
                    defaultValue = "ALL"
                }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filterType") ?: "ALL"
            AppListScreen(
                apps = apps,
                userMode = userMode,
                language = language,
                filterType = filter,
                onAppClick = { pkg ->
                    navController.navigate(Routes.appDetail(pkg))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.APP_DETAIL,
            arguments = listOf(
                navArgument("packageName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
            val selectedApp = apps.find { it.packageName == pkg }
            AppDetailScreen(
                app = selectedApp,
                userMode = userMode,
                language = language,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PANIC_SCREEN) {
            PanicScreen(
                apps = apps,
                language = language,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DORMANT_LIST) {
            DormantDangerScreen(
                apps = apps,
                language = language,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SHARE_SCREEN) {
            ShareFamilyScreen(
                apps = apps,
                deviceGrade = deviceGrade,
                deviceScore = deviceScore,
                initialLanguage = language,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRIVACY_SCREEN) {
            PrivacyModeScreen(
                selfProfile = selfProfile,
                language = language,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
