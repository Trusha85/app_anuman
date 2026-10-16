package com.appanuman.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appanuman.R
import com.appanuman.model.UserMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectorBottomSheet(
    currentMode: UserMode,
    onModeSelected: (UserMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.mode_selector_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Mode 1: Simple Mode
            ModeOptionRow(
                title = stringResource(id = R.string.mode_simple_title),
                description = stringResource(id = R.string.mode_simple_desc),
                icon = Icons.Default.Security,
                isSelected = currentMode == UserMode.SIMPLE,
                onClick = {
                    onModeSelected(UserMode.SIMPLE)
                    onDismiss()
                }
            )

            // Mode 2: Detailed Mode
            ModeOptionRow(
                title = stringResource(id = R.string.mode_detailed_title),
                description = stringResource(id = R.string.mode_detailed_desc),
                icon = Icons.Default.Psychology,
                isSelected = currentMode == UserMode.DETAILED,
                onClick = {
                    onModeSelected(UserMode.DETAILED)
                    onDismiss()
                }
            )

            // Mode 3: Grandparent Mode
            ModeOptionRow(
                title = stringResource(id = R.string.mode_grandparent_title),
                description = stringResource(id = R.string.mode_grandparent_desc),
                icon = Icons.Default.Elderly,
                isSelected = currentMode == UserMode.GRANDPARENT,
                onClick = {
                    onModeSelected(UserMode.GRANDPARENT)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModeOptionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
