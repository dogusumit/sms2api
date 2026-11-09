package com.sms2api.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sms2api.app.R
import com.sms2api.app.data.model.AuthType
import com.sms2api.app.data.model.FilterMode
import com.sms2api.app.ui.theme.*
import com.sms2api.app.ui.viewmodel.MainUiState
import com.sms2api.app.ui.viewmodel.MainViewModel
import com.sms2api.app.util.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    uiState: MainUiState
) {
    val context = LocalContext.current
    var showTlsInfoDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var expandedAuthDropdown by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Section: Target & Authentication
        item {
            Text(
                text = stringResource(R.string.section_target_auth),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Webhook URL
                    OutlinedTextField(
                        value = uiState.apiEndpoint,
                        onValueChange = { viewModel.updateApiEndpoint(it) },
                        label = { Text(stringResource(R.string.field_webhook_url)) },
                        placeholder = { Text(stringResource(R.string.field_webhook_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Auth Method Dropdown with Change Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val authLabels = mapOf(
                            AuthType.NONE to stringResource(R.string.auth_none),
                            AuthType.BASIC_AUTH to stringResource(R.string.auth_basic),
                            AuthType.BEARER_TOKEN to stringResource(R.string.auth_bearer),
                            AuthType.CUSTOM_HEADER to stringResource(R.string.auth_custom)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedAuthDropdown,
                            onExpandedChange = { expandedAuthDropdown = !expandedAuthDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = authLabels[uiState.authType] ?: uiState.authType.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.field_auth_method)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAuthDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = expandedAuthDropdown,
                                onDismissRequest = { expandedAuthDropdown = false }
                            ) {
                                listOf(
                                    AuthType.NONE,
                                    AuthType.BASIC_AUTH,
                                    AuthType.BEARER_TOKEN,
                                    AuthType.CUSTOM_HEADER
                                ).forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(authLabels[type] ?: type.name) },
                                        onClick = {
                                            viewModel.updateAuthType(type)
                                            expandedAuthDropdown = false
                                            if (type != AuthType.NONE) {
                                                showAuthDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (uiState.authType != AuthType.NONE) {
                            OutlinedButton(
                                onClick = { showAuthDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text(stringResource(R.string.btn_change), fontSize = 12.sp)
                            }
                        }
                    }

                    // Test Connection & Save Configuration Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testConnection() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isTestingConnection,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_testing), fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_test_connection), fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.saveSettings() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_save_config), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.connectionTestResult != null) {
                        Text(
                            text = uiState.connectionTestResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.connectionTestResult.startsWith("Success")) SuccessGreen else ErrorRose,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section: SMS Filtering Rules
        item {
            Text(
                text = stringResource(R.string.section_filtering_rules),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val pastSenders = uiState.records.map { it.sender }.distinct()

                    SenderMultiSelectDropdown(
                        selectedSendersStr = uiState.whitelistNumbers,
                        availableSenders = pastSenders,
                        onSelectionChanged = { newSelection ->
                            viewModel.updateWhitelistNumbers(newSelection)
                            viewModel.updateFilterMode(if (newSelection.isBlank()) FilterMode.ALL else FilterMode.SELECTED_NUMBERS)
                        }
                    )

                    OutlinedTextField(
                        value = uiState.keywordFilter,
                        onValueChange = { viewModel.updateKeywordFilter(it) },
                        label = { Text(stringResource(R.string.field_keyword_filter)) },
                        placeholder = { Text(stringResource(R.string.keyword_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Section: Reliability & OS Survival
        item {
            Text(
                text = stringResource(R.string.section_reliability),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Auto-Start
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.setting_autostart_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = stringResource(R.string.setting_autostart_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.isBootAutostartEnabled,
                            onCheckedChange = { viewModel.updateBootAutostart(it) },
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    HorizontalDivider()

                    // Periodic Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.setting_sync_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = stringResource(R.string.setting_sync_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.isPeriodicSyncEnabled,
                            onCheckedChange = { viewModel.updatePeriodicSync(it) },
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    HorizontalDivider()

                    // Battery Optimization Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.setting_battery_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (uiState.isBatteryOptimizationIgnored) stringResource(R.string.battery_exempted) else stringResource(R.string.battery_not_exempted),
                                fontSize = 11.sp,
                                color = if (uiState.isBatteryOptimizationIgnored) SuccessGreen else WarningAmber
                            )
                        }
                        if (!uiState.isBatteryOptimizationIgnored) {
                            OutlinedButton(
                                onClick = { requestBatteryOptimizationExemption(context) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.btn_configure), fontSize = 11.sp)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreenLight
                            ) {
                                Text(
                                    text = stringResource(R.string.battery_completed),
                                    color = SuccessGreenContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Service Status
        item {
            Text(
                text = stringResource(R.string.section_service_status),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            ServiceStatusHeroCard(
                isRunning = uiState.isServiceRunning,
                onToggleService = { viewModel.toggleService(context) }
            )
        }

        // Section: Language Selection
        item {
            Text(
                text = stringResource(R.string.section_language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            val currentLangObj = remember(uiState.appLanguage) {
                LocaleHelper.SUPPORTED_LANGUAGES.find { it.code == uiState.appLanguage }
                    ?: LocaleHelper.SUPPORTED_LANGUAGES.first()
            }

            Card(
                onClick = { showLanguageDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.language_selector_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentLangObj.displayLabel,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.language_selector_title))
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(LocaleHelper.SUPPORTED_LANGUAGES) { lang ->
                        val isSelected = uiState.appLanguage == lang.code
                        Surface(
                            onClick = {
                                viewModel.updateAppLanguage(context, lang.code)
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lang.nameInOwnLanguage,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (lang.code.isNotEmpty()) {
                                        Text(
                                            text = lang.nameInEnglish,
                                            fontSize = 11.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_done))
                }
            }
        )
    }

    // TLS 1.2+ Security Info Dialog
    if (showTlsInfoDialog) {
        AlertDialog(
            onDismissRequest = { showTlsInfoDialog = false },
            title = { Text(stringResource(R.string.dialog_tls_title)) },
            text = {
                Text(stringResource(R.string.dialog_tls_desc))
            },
            confirmButton = {
                TextButton(onClick = { showTlsInfoDialog = false }) {
                    Text(stringResource(R.string.btn_got_it))
                }
            }
        )
    }

    // Auth Configuration Dialog Popup
    if (showAuthDialog) {
        var tempUsername by remember { mutableStateOf(uiState.basicUsername) }
        var tempPassword by remember { mutableStateOf(uiState.basicPassword) }
        var tempToken by remember { mutableStateOf(uiState.bearerToken) }

        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = {
                Text(
                    text = when (uiState.authType) {
                        AuthType.BASIC_AUTH -> stringResource(R.string.dialog_auth_basic_title)
                        AuthType.BEARER_TOKEN -> stringResource(R.string.dialog_auth_bearer_title)
                        AuthType.CUSTOM_HEADER -> stringResource(R.string.dialog_auth_custom_title)
                        else -> stringResource(R.string.dialog_auth_default_title)
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (uiState.authType) {
                        AuthType.BASIC_AUTH -> {
                            OutlinedTextField(
                                value = tempUsername,
                                onValueChange = { tempUsername = it },
                                label = { Text(stringResource(R.string.field_username)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tempPassword,
                                onValueChange = { tempPassword = it },
                                label = { Text(stringResource(R.string.field_password)) },
                                placeholder = { Text(stringResource(R.string.field_leave_blank_clear)) },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                        AuthType.BEARER_TOKEN -> {
                            OutlinedTextField(
                                value = tempToken,
                                onValueChange = { tempToken = it },
                                label = { Text(stringResource(R.string.field_bearer_token)) },
                                placeholder = { Text(stringResource(R.string.field_leave_blank_clear)) },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                        AuthType.CUSTOM_HEADER -> {
                            Text(
                                text = stringResource(R.string.label_custom_headers_array),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            uiState.customHeaders.forEachIndexed { index, header ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = header.key,
                                        onValueChange = { newKey ->
                                            viewModel.updateCustomHeaderItem(index, newKey, header.value)
                                        },
                                        placeholder = { Text("X-API-Key") },
                                        modifier = Modifier.weight(0.45f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = header.value,
                                        onValueChange = { newVal ->
                                            viewModel.updateCustomHeaderItem(index, header.key, newVal)
                                        },
                                        placeholder = { Text("token") },
                                        modifier = Modifier.weight(0.45f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeCustomHeader(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorRose)
                                    }
                                }
                            }

                            TextButton(
                                onClick = { viewModel.addCustomHeader() },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_add_header))
                            }
                        }
                        else -> {
                            Text(stringResource(R.string.auth_no_config_needed))
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAuthDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBasicUsername(tempUsername)
                        viewModel.updateBasicPassword(tempPassword)
                        viewModel.updateBearerToken(tempToken)
                        showAuthDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_done))
                }
            }
        )
    }
}

@Composable
private fun ServiceStatusHeroCard(
    isRunning: Boolean,
    onToggleService: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) IndigoLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) SuccessGreen else Slate400)
                )
                Column {
                    Text(
                        text = if (isRunning) stringResource(R.string.service_active_status) else stringResource(R.string.service_stopped_status),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) PrimaryIndigoDark else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) stringResource(R.string.service_active_sub) else stringResource(R.string.service_stopped_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isRunning,
                onCheckedChange = { onToggleService() },
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}

@Composable
private fun SenderMultiSelectDropdown(
    selectedSendersStr: String,
    availableSenders: List<String>,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val selectedList = remember(selectedSendersStr) {
        if (selectedSendersStr.isBlank()) emptySet()
        else selectedSendersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val allAvailableSenders = remember(availableSenders, selectedList) {
        (selectedList.toList() + availableSenders).distinct()
    }

    val defaultAllText = stringResource(R.string.all_senders_default)
    val displayLabel = if (selectedList.isEmpty()) {
        defaultAllText
    } else {
        selectedList.joinToString(", ")
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.field_sender_filter), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = displayLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                title = { Text(stringResource(R.string.dialog_select_senders)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Inline Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_senders_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        // All Senders Option (Default)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChanged("")
                                    expanded = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedList.isEmpty(),
                                onClick = {
                                    onSelectionChanged("")
                                    expanded = false
                                }
                            )
                            Text(text = defaultAllText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        HorizontalDivider()

                        val filteredSenders = allAvailableSenders.filter {
                            it.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredSenders) { sender ->
                                val isChecked = selectedList.contains(sender)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newSet = selectedList.toMutableSet()
                                            if (isChecked) newSet.remove(sender) else newSet.add(sender)
                                            onSelectionChanged(newSet.joinToString(", "))
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val newSet = selectedList.toMutableSet()
                                            if (checked) newSet.add(sender) else newSet.remove(sender)
                                            onSelectionChanged(newSet.joinToString(", "))
                                        }
                                    )
                                    Text(text = sender, fontSize = 13.sp)
                                }
                            }

                            // Create on search if query is not empty and not in list
                            if (searchQuery.isNotBlank() && !allAvailableSenders.contains(searchQuery.trim())) {
                                item {
                                    TextButton(
                                        onClick = {
                                            val newSet = selectedList.toMutableSet()
                                            newSet.add(searchQuery.trim())
                                            onSelectionChanged(newSet.joinToString(", "))
                                            searchQuery = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.action_add_sender, searchQuery.trim()))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { expanded = false }) {
                        Text(stringResource(R.string.btn_done))
                    }
                }
            )
        }
    }
}

@SuppressLint("BatteryLife")
private fun requestBatteryOptimizationExemption(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
