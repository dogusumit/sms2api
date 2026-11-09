package com.sms2api.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sms2api.app.R
import com.sms2api.app.data.model.DeliveryStatus
import com.sms2api.app.data.model.LogFilterCategory
import com.sms2api.app.data.model.SmsRecord
import com.sms2api.app.ui.theme.*
import com.sms2api.app.ui.viewmodel.MainUiState
import com.sms2api.app.ui.viewmodel.MainViewModel
import com.sms2api.app.util.UrlValidator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onNavigateToSettings: () -> Unit
) {
    var recordToConfirmRetry by remember { mutableStateOf<SmsRecord?>(null) }

    fun triggerResendWithUrlCheck(record: SmsRecord) {
        if (!UrlValidator.isValidUrl(uiState.apiEndpoint)) {
            viewModel.showSnackbarMessage("Lütfen önce Ayarlar ekranından geçerli bir Webhook URL tanımlayın!")
            onNavigateToSettings()
        } else {
            recordToConfirmRetry = record
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Search Header (Full width)
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Category Chips (Compact)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == LogFilterCategory.ALL,
                    onClick = { viewModel.setFilterCategory(LogFilterCategory.ALL) },
                    label = { Text(stringResource(R.string.filter_all, uiState.totalCount), fontSize = 12.sp) }
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == LogFilterCategory.MATCHED,
                    onClick = { viewModel.setFilterCategory(LogFilterCategory.MATCHED) },
                    label = { Text(stringResource(R.string.filter_matched, uiState.matchedCount), fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PrimaryIndigo)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == LogFilterCategory.SKIPPED,
                    onClick = { viewModel.setFilterCategory(LogFilterCategory.SKIPPED) },
                    label = { Text(stringResource(R.string.filter_skipped, uiState.skippedCount), fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Slate600)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == LogFilterCategory.SUCCESS,
                    onClick = { viewModel.setFilterCategory(LogFilterCategory.SUCCESS) },
                    label = { Text(stringResource(R.string.filter_success, uiState.successCount), fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SuccessGreen)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == LogFilterCategory.PENDING,
                    onClick = { viewModel.setFilterCategory(LogFilterCategory.PENDING) },
                    label = { Text(stringResource(R.string.filter_pending, uiState.pendingCount), fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(WarningAmber)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == LogFilterCategory.FAILED,
                    onClick = { viewModel.setFilterCategory(LogFilterCategory.FAILED) },
                    label = { Text(stringResource(R.string.filter_failed, uiState.failedCount), fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ErrorRose)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Records List
        if (uiState.filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedFilterCategory != LogFilterCategory.ALL) {
                        stringResource(R.string.empty_logs_filtered)
                    } else {
                        stringResource(R.string.empty_logs_initial)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredRecords, key = { it.messageId }) { record ->
                    SmsRecordItemCard(
                        record = record,
                        onResendClick = { triggerResendWithUrlCheck(record) }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Individual Record Resend
    recordToConfirmRetry?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToConfirmRetry = null },
            title = { Text(stringResource(R.string.dialog_send_record_title)) },
            text = { Text(stringResource(R.string.dialog_send_record_desc, record.sender)) },
            dismissButton = {
                TextButton(onClick = { recordToConfirmRetry = null }) {
                    Text(stringResource(R.string.dialog_btn_no))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.retryRecord(record.messageId)
                        recordToConfirmRetry = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_btn_yes))
                }
            }
        )
    }
}

@Composable
private fun SmsRecordItemCard(
    record: SmsRecord,
    onResendClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(record.timestamp))
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Sender + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.sender,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Badges row: Status, HTTP Code, API Send attempts
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                val (statusBg, statusFg) = when (record.status) {
                    DeliveryStatus.SUCCESS.name -> SuccessGreenLight to SuccessGreenContainer
                    DeliveryStatus.PENDING.name -> WarningAmberLight to WarningAmberContainer
                    DeliveryStatus.SKIPPED.name -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    else -> ErrorRoseLight to ErrorRoseContainer
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusBg
                ) {
                    Text(
                        text = if (record.status == DeliveryStatus.SKIPPED.name) stringResource(R.string.badge_skipped) else record.status,
                        color = statusFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }

                // HTTP Code
                if (record.httpCode != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.badge_http, record.httpCode),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                // API Send Attempts
                val attempts = if (record.httpCode != null || record.retryCount > 0 || record.status == DeliveryStatus.SUCCESS.name || record.status == DeliveryStatus.FAILED.name) record.retryCount + 1 else 0
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = stringResource(R.string.badge_sent_attempts, attempts),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body preview (click to toggle expansion)
            Text(
                text = record.body,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Message ID hash + Send to API button
            val messageIdLabel = stringResource(R.string.label_message_id)
            val copiedToastText = stringResource(R.string.toast_copied, messageIdLabel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // message_id Hash
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        copyToClipboard(context, messageIdLabel, record.messageId, copiedToastText)
                    }
                ) {
                    Text(
                        text = record.messageId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_message_id),
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Send to API Button
                TextButton(
                    onClick = onResendClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_send_to_api), fontSize = 11.sp)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
