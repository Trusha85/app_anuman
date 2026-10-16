package com.appanuman.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.CategoryStatus
import com.appanuman.model.PermissionCategory
import com.appanuman.model.UserMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGlossarySheet(
    category: PermissionCategory,
    status: CategoryStatus,
    permissionsList: List<String>,
    userMode: UserMode,
    onDismiss: () -> Unit
) {
    val (categoryTitleRes, analogyRes) = when (category) {
        PermissionCategory.SPECIAL_ACCESS -> Pair(R.string.cat_special_access, R.string.analogy_special_access)
        PermissionCategory.SMS -> Pair(R.string.cat_sms, R.string.analogy_sms)
        PermissionCategory.CONTACTS_CALLS -> Pair(R.string.cat_contacts_calls, R.string.analogy_contacts_calls)
        PermissionCategory.CAMERA_MIC -> Pair(R.string.cat_camera_mic, R.string.analogy_camera_mic)
        PermissionCategory.LOCATION -> Pair(R.string.cat_location, R.string.analogy_location)
        PermissionCategory.PHONE_IDENTITY -> Pair(R.string.cat_phone_identity, R.string.analogy_phone_identity)
        PermissionCategory.STORAGE -> Pair(R.string.cat_storage, R.string.analogy_storage)
        PermissionCategory.BACKGROUND -> Pair(R.string.cat_background, R.string.analogy_background)
        PermissionCategory.NETWORK -> Pair(R.string.cat_network, R.string.analogy_network)
    }

    val statusTextRes = when (status) {
        CategoryStatus.GRANTED -> R.string.status_granted
        CategoryStatus.DECLARED_ONLY -> R.string.status_declared_only
        CategoryStatus.NOT_REQUESTED -> R.string.status_not_requested
        CategoryStatus.UNABLE_TO_CHECK -> R.string.status_unable_to_check
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = categoryTitleRes),
                style = if (userMode == UserMode.GRANDPARENT) {
                    MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                },
                color = MaterialTheme.colorScheme.onSurface
            )

            // Plain analogy card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.glossary_analogy_header),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "“" + stringResource(id = analogyRes) + "”",
                        style = if (userMode == UserMode.GRANDPARENT) {
                            MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontStyle = FontStyle.Italic)
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Current status
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.glossary_status_header),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = statusTextRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Individual permissions list (in Detailed mode)
            if (userMode == UserMode.DETAILED && permissionsList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.glossary_declared_permissions),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    permissionsList.forEach { perm ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = perm,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .minimumInteractiveComponentSize(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.glossary_close),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
