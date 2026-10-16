package com.appanuman.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.CategoryStatus
import com.appanuman.model.PermissionCategory
import com.appanuman.model.UserMode
import com.appanuman.ui.theme.LocalRiskColors

@Composable
fun CapabilityGrid(
    statuses: Map<PermissionCategory, CategoryStatus>,
    userMode: UserMode,
    onCategoryClick: (PermissionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        PermissionCategory.SPECIAL_ACCESS,
        PermissionCategory.SMS,
        PermissionCategory.CONTACTS_CALLS,
        PermissionCategory.CAMERA_MIC,
        PermissionCategory.LOCATION,
        PermissionCategory.PHONE_IDENTITY,
        PermissionCategory.STORAGE,
        PermissionCategory.BACKGROUND,
        PermissionCategory.NETWORK
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (rowIndex in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (colIndex in 0 until 3) {
                    val category = categories[rowIndex * 3 + colIndex]
                    val status = statuses[category] ?: CategoryStatus.NOT_REQUESTED

                    Box(modifier = Modifier.weight(1f)) {
                        CapabilityCell(
                            category = category,
                            status = status,
                            userMode = userMode,
                            onClick = { onCategoryClick(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityCell(
    category: PermissionCategory,
    status: CategoryStatus,
    userMode: UserMode,
    onClick: () -> Unit
) {
    val riskColors = LocalRiskColors.current
    val (icon, titleRes) = getCategoryMeta(category)

    val (statusBg, statusBorder, statusTextRes) = when (status) {
        CategoryStatus.GRANTED -> Triple(riskColors.high, null, R.string.status_granted)
        CategoryStatus.DECLARED_ONLY -> Triple(Color.Transparent, BorderStroke(1.5.dp, riskColors.medium), R.string.status_declared_only)
        CategoryStatus.NOT_REQUESTED -> Triple(MaterialTheme.colorScheme.surfaceVariant, null, R.string.status_not_requested)
        CategoryStatus.UNABLE_TO_CHECK -> Triple(Color.Transparent, BorderStroke(1.dp, MaterialTheme.colorScheme.outline), R.string.status_unable_to_check)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterVertically,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (status == CategoryStatus.GRANTED) riskColors.high else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Status chip/bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(color = statusBg, shape = RoundedCornerShape(4.dp))
                    .then(
                        if (statusBorder != null) Modifier.border(statusBorder, RoundedCornerShape(4.dp)) else Modifier
                    )
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (status == CategoryStatus.UNABLE_TO_CHECK) "? " + stringResource(id = statusTextRes) else stringResource(id = statusTextRes),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = when (status) {
                        CategoryStatus.GRANTED -> Color.White
                        CategoryStatus.DECLARED_ONLY -> riskColors.medium
                        CategoryStatus.NOT_REQUESTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        CategoryStatus.UNABLE_TO_CHECK -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

private fun getCategoryMeta(category: PermissionCategory): Pair<ImageVector, Int> {
    return when (category) {
        PermissionCategory.SPECIAL_ACCESS -> Pair(Icons.Default.AdminPanelSettings, R.string.cat_special_access)
        PermissionCategory.SMS -> Pair(Icons.Default.Sms, R.string.cat_sms)
        PermissionCategory.CONTACTS_CALLS -> Pair(Icons.Default.Contacts, R.string.cat_contacts_calls)
        PermissionCategory.CAMERA_MIC -> Pair(Icons.Default.Videocam, R.string.cat_camera_mic)
        PermissionCategory.LOCATION -> Pair(Icons.Default.LocationOn, R.string.cat_location)
        PermissionCategory.PHONE_IDENTITY -> Pair(Icons.Default.PermPhoneMsg, R.string.cat_phone_identity)
        PermissionCategory.STORAGE -> Pair(Icons.Default.Folder, R.string.cat_storage)
        PermissionCategory.BACKGROUND -> Pair(Icons.Default.Sync, R.string.cat_background)
        PermissionCategory.NETWORK -> Pair(Icons.Default.Language, R.string.cat_network)
    }
}
