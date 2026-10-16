package com.appanuman.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.appanuman.R
import com.appanuman.model.UserMode
import com.appanuman.ui.navigation.NavRoutes

@Composable
fun AppBottomBar(
    currentRoute: String?,
    userMode: UserMode,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.height(72.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(NavRoutes.HOME, Icons.Default.Home, R.string.nav_home),
            Triple(NavRoutes.appListRoute("all"), Icons.Default.Apps, R.string.nav_apps),
            Triple(NavRoutes.DORMANT_LIST, Icons.Default.Bedtime, R.string.nav_dormant),
            Triple(NavRoutes.ABOUT_SCREEN, Icons.Default.Info, R.string.nav_about)
        )

        items.forEach { (route, icon, labelRes) ->
            val isSelected = when (route) {
                NavRoutes.HOME -> currentRoute == NavRoutes.HOME
                NavRoutes.DORMANT_LIST -> currentRoute == NavRoutes.DORMANT_LIST
                NavRoutes.ABOUT_SCREEN -> currentRoute == NavRoutes.ABOUT_SCREEN
                else -> currentRoute?.startsWith("appList") == true
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(id = labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = labelRes),
                        style = if (userMode == UserMode.GRANDPARENT) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.labelMedium
                        }
                    )
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
