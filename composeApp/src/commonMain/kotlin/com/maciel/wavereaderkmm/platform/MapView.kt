package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel

/**
 * Platform-specific map component
 *
 * Android: Google Maps
 * iOS: MapKit
 */
@Composable
expect fun MapView(
    locationViewModel: LocationViewModel,
    modifier: Modifier = Modifier
)