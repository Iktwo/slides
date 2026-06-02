package com.iktwo.slides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import androidx.compose.ui.graphics.ImageBitmap

enum class DisplayMode(val label: String, val description: String) {
    FitSingle(
        label = "Single · Fit",
        description = "One image at a time, scaled to fit without cropping. Black bars may appear.",
    ),
    FillSingle(
        label = "Single · Fill",
        description = "One image at a time, scaled to fill the screen. Edges may be cropped.",
    ),
    PhasedGrid(
        label = "Phased Grid",
        description = "Multiple images tiled in a varied grid layout. Each tile cropped to fit its cell.",
    ),
    PhasedGridZoom(
        label = "Phased Grid · Zoom",
        description = "Multiple images in a varied grid, each zoomed past fill around a focal point.",
    ),
}

class SlideshowState {
    val images = mutableStateListOf<ImageBitmap>()
    var mode by mutableStateOf(DisplayMode.FitSingle)
    var gridCount by mutableStateOf(6)
    var random by mutableStateOf(true)
    var repeat by mutableStateOf(true)
    var shiftLayout by mutableStateOf(false)
    var intervalMs by mutableStateOf(3000L)
    var started by mutableStateOf(false)
}

@Composable
fun rememberSlideshowState(): SlideshowState {
    val state = remember {
        SlideshowState().apply { applyConfig(loadConfigString()) }
    }
    LaunchedEffect(state) {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        snapshotFlow {
            state.serializeConfig()
        }.debounce(200).collectLatest { saveConfigString(it) }
    }
    return state
}
