package com.maciel.wavereaderkmm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maciel.wavereaderkmm.data.FirebaseAuthRepository
import com.maciel.wavereaderkmm.data.FirestoreRepository
import com.maciel.wavereaderkmm.data.NetworkWaveApiRepository
import com.maciel.wavereaderkmm.platform.rememberLocationService
import com.maciel.wavereaderkmm.platform.rememberSensorDataSource
import com.maciel.wavereaderkmm.ui.auth.LoginScreen
import com.maciel.wavereaderkmm.ui.auth.RegisterScreen
import com.maciel.wavereaderkmm.ui.auth.StartScreen
import com.maciel.wavereaderkmm.ui.main.HistoryScreen
import com.maciel.wavereaderkmm.ui.main.InfoScreen
import com.maciel.wavereaderkmm.ui.main.MainScreen
import com.maciel.wavereaderkmm.viewmodels.HistoryViewModel
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.SensorViewModel
import com.maciel.wavereaderkmm.viewmodels.ServiceViewModel
import kotlinx.coroutines.launch

/**
 * Complete WaveReader App Navigation
 *
 * Handles:
 * - Auth flow (Start → Login/Register)
 * - Main app (Record, Search tabs)
 * - History screen
 * - Info screen
 * - Guest mode
 */
@Composable
fun WaveReaderApp() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Repositories
    val authRepository = remember { FirebaseAuthRepository() }
    val firestoreRepository = remember { FirestoreRepository() }
    val waveApiRepository = remember { NetworkWaveApiRepository() }

    // Platform-specific services
    val sensorDataSource = rememberSensorDataSource()
    val locationService = rememberLocationService()

    // ViewModels
    val sensorViewModel = viewModel {
        SensorViewModel(sensorDataSource, firestoreRepository)
    }

    val locationViewModel = viewModel {
        LocationViewModel(locationService)
    }

    val serviceViewModel = viewModel {
        ServiceViewModel(waveApiRepository)
    }

    val historyViewModel = viewModel {
        HistoryViewModel(firestoreRepository)
    }

    // Track guest mode
    var isGuest by remember { mutableStateOf(false) }

    // Check if user is signed in
    val startDestination = if (authRepository.isSignedIn) {
        "main"
    } else {
        "start"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Start Screen
        composable("start") {
            StartScreen(
                onLogin = { navController.navigate("login") },
                onRegister = { navController.navigate("register") },
                onGuest = {
                    isGuest = true
                    navController.navigate("main") {
                        popUpTo("start") { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable("login") {
            LoginScreen(
                auth = authRepository,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    isGuest = false
                    navController.navigate("main") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onRegisterNavigate = {
                    navController.navigate("register") {
                        popUpTo("start")
                    }
                }
            )
        }

        // Register Screen
        composable("register") {
            RegisterScreen(
                auth = authRepository,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    isGuest = false
                    navController.navigate("main") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onLoginNavigate = {
                    navController.navigate("login") {
                        popUpTo("start")
                    }
                }
            )
        }

        // Main Screen (with tabs)
        composable("main") {
            MainScreen(
                sensorViewModel = sensorViewModel,
                locationViewModel = locationViewModel,
                serviceViewModel = serviceViewModel,
                onSignOut = {
                    scope.launch {
                        authRepository.signOut()
                        isGuest = false
                        navController.navigate("start") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onHistoryNavigate = {
                    navController.navigate("history")
                },
                onInfoNavigate = {
                    navController.navigate("info")
                },
                isGuest = isGuest
            )
        }

        // History Screen
        composable("history") {
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Info Screen
        composable("info") {
            InfoScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}