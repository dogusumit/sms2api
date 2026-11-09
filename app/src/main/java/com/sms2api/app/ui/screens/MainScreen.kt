package com.sms2api.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sms2api.app.R
import com.sms2api.app.ui.viewmodel.MainViewModel

enum class NavigationTab {
    DASHBOARD,
    LOGS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSnackbar()
        }
    }

    val currentTabTitle = when (selectedTab) {
        NavigationTab.DASHBOARD -> stringResource(R.string.nav_dashboard)
        NavigationTab.LOGS -> stringResource(R.string.nav_logs)
        NavigationTab.SETTINGS -> stringResource(R.string.nav_settings)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = stringResource(R.string.app_name),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = stringResource(R.string.title_format, currentTabTitle),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DASHBOARD,
                    onClick = { selectedTab = NavigationTab.DASHBOARD },
                    icon = {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.nav_dashboard)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_dashboard)) }
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.LOGS,
                    onClick = { selectedTab = NavigationTab.LOGS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.pendingCount > 0) {
                                    Badge {
                                        Text(uiState.pendingCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = stringResource(R.string.nav_logs_short)
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.nav_logs_short)) }
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SETTINGS,
                    onClick = { selectedTab = NavigationTab.SETTINGS },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings_short)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings_short)) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onNavigateToLogs = { targetCategory ->
                        viewModel.setFilterCategory(targetCategory)
                        selectedTab = NavigationTab.LOGS
                    },
                    onNavigateToSettings = {
                        selectedTab = NavigationTab.SETTINGS
                    }
                )
                NavigationTab.LOGS -> LogsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onNavigateToSettings = {
                        selectedTab = NavigationTab.SETTINGS
                    }
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }
}
