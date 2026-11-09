package com.sms2api.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.sms2api.app.util.UrlValidator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sms2api.app.R
import com.sms2api.app.data.model.LogFilterCategory
import com.sms2api.app.ui.theme.*
import com.sms2api.app.ui.viewmodel.MainUiState
import com.sms2api.app.ui.viewmodel.MainViewModel

sealed class DashboardAction {
    object HistoricalImport : DashboardAction()
    object ResendAll : DashboardAction()
    object SendPending : DashboardAction()
    object SendFailed : DashboardAction()
    object TestApi : DashboardAction()
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onNavigateToLogs: (LogFilterCategory) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var activeAction by remember { mutableStateOf<DashboardAction?>(null) }

    fun triggerActionWithUrlCheck(action: DashboardAction) {
        if (!UrlValidator.isValidUrl(uiState.apiEndpoint)) {
            viewModel.showSnackbarMessage("Lütfen önce Ayarlar ekranından geçerli bir Webhook URL tanımlayın!")
            onNavigateToSettings()
        } else {
            activeAction = action
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Card: Foreground Service Status & Control
        ServiceStatusHeroCard(
            isRunning = uiState.isServiceRunning,
            onToggleService = {
                if (!uiState.isServiceRunning) {
                    if (!UrlValidator.isValidUrl(uiState.apiEndpoint)) {
                        viewModel.showSnackbarMessage("Lütfen önce Ayarlar ekranından geçerli bir Webhook URL tanımlayın!")
                        onNavigateToSettings()
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.toggleService(context)
                    } else {
                        viewModel.showSnackbarMessage("SMS alma izni gerekli. Lütfen ayarlardan izin verin.")
                    }
                } else {
                    viewModel.toggleService(context)
                }
            }
        )

        // Battery Optimization Warning Banner
        if (!uiState.isBatteryOptimizationIgnored) {
            BatteryOptimizationBanner(
                onDisableOptimization = {
                    requestDisableBatteryOptimization(context)
                }
            )
        }

        // Section: Real-Time Metrics
        Text(
            text = stringResource(R.string.section_metrics),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.metric_matched),
                count = uiState.matchedCount,
                icon = Icons.Default.FilterList,
                accentColor = PrimaryIndigo,
                bgColor = IndigoLight,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToLogs(LogFilterCategory.MATCHED) }
            )
            MetricCard(
                title = stringResource(R.string.metric_skipped),
                count = uiState.skippedCount,
                icon = Icons.Default.FilterListOff,
                accentColor = Slate600,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToLogs(LogFilterCategory.SKIPPED) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.metric_delivered),
                count = uiState.successCount,
                icon = Icons.Default.CheckCircle,
                accentColor = SuccessGreen,
                bgColor = SuccessGreenLight,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToLogs(LogFilterCategory.SUCCESS) }
            )
            MetricCard(
                title = stringResource(R.string.metric_pending),
                count = uiState.pendingCount,
                icon = Icons.Default.Schedule,
                accentColor = WarningAmber,
                bgColor = WarningAmberLight,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToLogs(LogFilterCategory.PENDING) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.metric_failed),
                count = uiState.failedCount,
                icon = Icons.Default.ErrorOutline,
                accentColor = ErrorRose,
                bgColor = ErrorRoseLight,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToLogs(LogFilterCategory.FAILED) }
            )
            MetricCard(
                title = stringResource(R.string.metric_total),
                count = uiState.totalCount,
                icon = Icons.Default.AllInclusive,
                accentColor = PrimaryIndigo,
                bgColor = IndigoLight,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToLogs(LogFilterCategory.ALL) }
            )
        }

        // Quick Actions Section
        Text(
            text = stringResource(R.string.section_actions),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Historical Bulk Transfer Button
                Button(
                    onClick = { triggerActionWithUrlCheck(DashboardAction.HistoricalImport) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !uiState.isHistoricalImporting,
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    if (uiState.isHistoricalImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_scanning_inbox), fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_sync_all_past), fontSize = 13.sp)
                    }
                }

                // Resend All & Send Pending
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { triggerActionWithUrlCheck(DashboardAction.ResendAll) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_resend_all), fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { triggerActionWithUrlCheck(DashboardAction.SendPending) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_send_pending), fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Send Failed & Test API
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { triggerActionWithUrlCheck(DashboardAction.SendFailed) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_send_failed), fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { triggerActionWithUrlCheck(DashboardAction.TestApi) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.isTestingConnection,
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_testing), fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_test_api), fontSize = 11.sp, maxLines = 1)
                        }
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

    // Confirmation Dialog for Dashboard Actions
    activeAction?.let { action ->
        val title: String
        val description: String
        val onConfirm: () -> Unit

        when (action) {
            DashboardAction.HistoricalImport -> {
                title = stringResource(R.string.dialog_sync_title)
                description = stringResource(R.string.dialog_sync_desc)
                onConfirm = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.importHistoricalMessages(context)
                    } else {
                        viewModel.showSnackbarMessage("Geçmiş SMS'ler için SMS okuma izni gerekli (Lütfen ayarlardan verin).")
                    }
                }
            }
            DashboardAction.ResendAll -> {
                title = stringResource(R.string.dialog_resend_all_title)
                description = stringResource(R.string.dialog_resend_all_desc)
                onConfirm = { viewModel.resendAllRecords(context) }
            }
            DashboardAction.SendPending -> {
                title = stringResource(R.string.dialog_send_pending_title)
                description = stringResource(R.string.dialog_send_pending_desc)
                onConfirm = { viewModel.flushQueue(context) }
            }
            DashboardAction.SendFailed -> {
                title = stringResource(R.string.dialog_send_failed_title)
                description = stringResource(R.string.dialog_send_failed_desc)
                onConfirm = { viewModel.retryAllFailed() }
            }
            DashboardAction.TestApi -> {
                title = stringResource(R.string.dialog_test_api_title)
                description = stringResource(R.string.dialog_test_api_desc)
                onConfirm = { viewModel.testConnection() }
            }
        }

        AlertDialog(
            onDismissRequest = { activeAction = null },
            title = { Text(title) },
            text = { Text(description) },
            dismissButton = {
                TextButton(onClick = { activeAction = null }) {
                    Text(stringResource(R.string.dialog_btn_no))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        activeAction = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_btn_yes))
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
private fun BatteryOptimizationBanner(
    onDisableOptimization: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.BatterySaver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.battery_opt_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.battery_opt_desc),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onDisableOptimization,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(stringResource(R.string.battery_opt_allow), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@SuppressLint("BatteryLife")
private fun requestDisableBatteryOptimization(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
