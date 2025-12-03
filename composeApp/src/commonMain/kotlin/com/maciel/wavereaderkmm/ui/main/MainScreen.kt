package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.SensorViewModel
import com.maciel.wavereaderkmm.viewmodels.ServiceViewModel
import org.jetbrains.compose.resources.stringResource
import wavereaderkmm.composeapp.generated.resources.Res
import wavereaderkmm.composeapp.generated.resources.titlename

/**
 * Main tabs enum
 */
enum class MainScreenTab(val label: String) {
    Measure("Measure Waves"),
    Search("Search Waves")
}

/**
 * Main Screen - Controls the main app navigation with tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sensorViewModel: SensorViewModel,
    locationViewModel: LocationViewModel,
    serviceViewModel: ServiceViewModel,
    onSignOut: () -> Unit,
    onHistoryNavigate: () -> Unit,
    onInfoNavigate: () -> Unit,
    isGuest: Boolean
) {
    val navController = rememberNavController()
    val uiState by sensorViewModel.uiState.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry?.destination?.route?.let { route ->
        MainScreenTab.entries.find { it.name == route }
    } ?: MainScreenTab.Measure

    val tabs = listOf(MainScreenTab.Measure, MainScreenTab.Search)
    val selectedTabIndex = rememberSaveable { mutableIntStateOf(tabs.indexOf(currentScreen)) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = stringResource(Res.string.titlename))
                    },
                    actions = {
                        DropDownMenuButton(
                            isGuest = isGuest,
                            onInfoNavigate = onInfoNavigate,
                            onHistoryNavigate = onHistoryNavigate,
                            onSignOut = onSignOut
                        )
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex.intValue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, screen ->
                        Tab(
                            selected = selectedTabIndex.intValue == index,
                            onClick = {
                                selectedTabIndex.intValue = index
                                navController.navigate(screen.name) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            text = { Text(screen.label) },
                            icon = {
                                Icon(
                                    imageVector = if (screen == MainScreenTab.Measure) {
                                        Icons.Default.PlayArrow
                                    } else {
                                        Icons.Default.Search
                                    },
                                    contentDescription = screen.name
                                )
                            },
                            selectedContentColor = Color.DarkGray,
                            unselectedContentColor = Color.LightGray
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainScreenTab.Measure.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainScreenTab.Measure.name) {
                RecordDataScreen(
                    sensorViewModel = sensorViewModel,
                    locationViewModel = locationViewModel,
                    isGuest = isGuest
                )
            }

            composable(MainScreenTab.Search.name) {
                SearchDataScreen(
                    locationViewModel = locationViewModel,
                    serviceViewModel = serviceViewModel
                )
            }
        }
    }
}

/**
 * Dropdown menu button in top app bar
 */
@Composable
fun DropDownMenuButton(
    isGuest: Boolean,
    onInfoNavigate: () -> Unit,
    onHistoryNavigate: () -> Unit,
    onSignOut: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(16.dp)) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // History (only for signed-in users)
            if (!isGuest) {
                DropdownMenuItem(
                    text = { Text("History") },
                    leadingIcon = {
                        Icon(Icons.Default.History, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onHistoryNavigate()
                    }
                )
            }

            // About
            DropdownMenuItem(
                text = { Text("About") },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onInfoNavigate()
                }
            )

            // Sign In/Out
            DropdownMenuItem(
                text = { Text(if (isGuest) "Sign In" else "Sign Out") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onSignOut()
                }
            )
        }
    }
}
