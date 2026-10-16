package com.appanuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.*
import com.appanuman.ui.components.*
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun AppListScreen(
    apps: List<AppRiskProfile>,
    userMode: UserMode,
    language: Language,
    searchQuery: String,
    selectedFilter: AppFilter,
    sortOrder: SortOrder,
    includeSystemApps: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onToggleSystemApps: (Boolean) -> Unit,
    onAppClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.list_title),
                showBackButton = true,
                showModeSelector = false,
                currentLanguage = language,
                userMode = userMode,
                onBackClick = onBackClick,
                onLanguageToggle = onToggleLanguage
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.list_search_hint),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // In Simple and Detailed modes: Filter chips row and Sort dropdown
            if (userMode != UserMode.GRANDPARENT) {
                // Filter chips row (horizontally scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_all),
                        isSelected = selectedFilter == AppFilter.ALL,
                        onClick = { onFilterChange(AppFilter.ALL) }
                    )
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_critical),
                        isSelected = selectedFilter == AppFilter.CRITICAL,
                        onClick = { onFilterChange(AppFilter.CRITICAL) }
                    )
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_high),
                        isSelected = selectedFilter == AppFilter.HIGH,
                        onClick = { onFilterChange(AppFilter.HIGH) }
                    )
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_dormant),
                        isSelected = selectedFilter == AppFilter.DORMANT,
                        onClick = { onFilterChange(AppFilter.DORMANT) }
                    )
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_sideloaded),
                        isSelected = selectedFilter == AppFilter.SIDELOADED,
                        onClick = { onFilterChange(AppFilter.SIDELOADED) }
                    )
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_mismatch),
                        isSelected = selectedFilter == AppFilter.MISMATCH,
                        onClick = { onFilterChange(AppFilter.MISMATCH) }
                    )
                    FilterChipItem(
                        label = stringResource(id = R.string.list_filter_creep),
                        isSelected = selectedFilter == AppFilter.CREEP,
                        onClick = { onFilterChange(AppFilter.CREEP) }
                    )
                }

                // Controls row: Sort dropdown & System apps toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort dropdown button
                    Box {
                        TextButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    id = when (sortOrder) {
                                        SortOrder.RISK_SCORE -> R.string.list_sort_risk
                                        SortOrder.NAME -> R.string.list_sort_name
                                        SortOrder.INSTALL_DATE -> R.string.list_sort_date
                                        SortOrder.DAYS_UNUSED -> R.string.list_sort_dormant
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.list_sort_risk)) },
                                onClick = { onSortChange(SortOrder.RISK_SCORE); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.list_sort_name)) },
                                onClick = { onSortChange(SortOrder.NAME); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.list_sort_date)) },
                                onClick = { onSortChange(SortOrder.INSTALL_DATE); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.list_sort_dormant)) },
                                onClick = { onSortChange(SortOrder.DAYS_UNUSED); showSortMenu = false }
                            )
                        }
                    }

                    // System apps toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.list_system_apps_toggle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = includeSystemApps,
                            onCheckedChange = onToggleSystemApps,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }

            // LazyColumn of App Items
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = apps, key = { it.packageName }) { app ->
                        if (userMode == UserMode.GRANDPARENT) {
                            GrandparentAppRow(app = app, onClick = { onAppClick(app.packageName) })
                        } else {
                            StandardAppRow(app = app, onClick = { onAppClick(app.packageName) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        modifier = Modifier.minimumInteractiveComponentSize(),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun StandardAppRow(
    app: AppRiskProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Icon 48dp
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    RiskBandChip(band = app.riskBand)
                }

                // Badges row: Trust badge, mismatch badge, creep badge, dormant days
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TrustBadge(trustLevel = app.trustLevel)

                    if (app.contextMismatch) {
                        ContextMismatchBadge()
                    }

                    if (app.permissionCreepFlag) {
                        CreepBadge()
                    }

                    if (app.isDormant) {
                        Text(
                            text = stringResource(id = R.string.dormant_days_unused, app.daysUnused),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrandparentAppRow(
    app: AppRiskProfile,
    onClick: () -> Unit
) {
    val riskColors = LocalRiskColors.current
    val dotColor = when (app.riskBand) {
        RiskBand.LOW -> riskColors.low
        RiskBand.MEDIUM -> riskColors.medium
        RiskBand.HIGH, RiskBand.CRITICAL -> riskColors.high
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon 64dp
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Plain app name in 22sp
            Text(
                text = app.appName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Single colored dot only (no text chips)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = dotColor, shape = CircleShape)
            )
        }
    }
}
