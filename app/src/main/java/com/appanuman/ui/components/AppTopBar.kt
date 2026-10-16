package com.appanuman.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appanuman.R
import com.appanuman.model.Language
import com.appanuman.model.UserMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String? = null,
    showBackButton: Boolean = false,
    showModeSelector: Boolean = false,
    currentLanguage: Language,
    userMode: UserMode,
    onBackClick: () -> Unit = {},
    onModeClick: () -> Unit = {},
    onLanguageToggle: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (title == null) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = if (userMode == UserMode.GRANDPARENT) {
                            MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Text(
                        text = title,
                        style = if (userMode == UserMode.GRANDPARENT) {
                            MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp)
                        },
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = {
            // Language Toggle: EN | हिं
            OutlinedButton(
                onClick = onLanguageToggle,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .padding(end = 6.dp)
                    .heightIn(min = 40.dp),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (currentLanguage == Language.EN) "हिं" else "EN",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Mode Selector Icon (on home only)
            if (showModeSelector) {
                IconButton(
                    onClick = onModeClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.DisplaySettings,
                        contentDescription = stringResource(id = R.string.mode_selector_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
