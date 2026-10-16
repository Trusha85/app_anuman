package com.appanuman.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val APP_LIST = "appList/{filter}"
    const val APP_DETAIL = "appDetail/{packageName}"
    const val PANIC_SCREEN = "panicScreen"
    const val DORMANT_LIST = "dormantList"
    const val SHARE_SCREEN = "shareScreen"
    const val ABOUT_SCREEN = "aboutScreen"

    fun appListRoute(filter: String = "all"): String = "appList/$filter"
    fun appDetailRoute(packageName: String): String = "appDetail/$packageName"
}
