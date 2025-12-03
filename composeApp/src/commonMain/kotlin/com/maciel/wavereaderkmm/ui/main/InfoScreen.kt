package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.ui.components.ExpandableInfoCard
import org.jetbrains.compose.resources.stringResource
import wavereaderkmm.composeapp.generated.resources.Res
import wavereaderkmm.composeapp.generated.resources.calculating_wave_direction_body
import wavereaderkmm.composeapp.generated.resources.calculating_wave_height_body
import wavereaderkmm.composeapp.generated.resources.calculating_wave_period_body
import wavereaderkmm.composeapp.generated.resources.info_api_body
import wavereaderkmm.composeapp.generated.resources.info_sensor_body

/**
 * Info Screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun InfoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About WaveReader") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sensor Use Card
            ElevatedCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sensor Use and Setup",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(Res.string.info_sensor_body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExpandableInfoCard(
                        title = "Calculating Wave Height",
                        bulletPoints = listOf(
                            stringResource(Res.string.calculating_wave_height_body)
                        )
                    )

                    ExpandableInfoCard(
                        title = "Calculating Wave Period",
                        bulletPoints = listOf(
                            stringResource(Res.string.calculating_wave_period_body)
                        )
                    )

                    ExpandableInfoCard(
                        title = "Calculating Wave Direction",
                        bulletPoints = listOf(
                            stringResource(Res.string.calculating_wave_direction_body)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // API Info Card
            ElevatedCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About the API",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(Res.string.info_api_body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
