package com.iktwo.slides

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState()
    var fullscreen by remember { mutableStateOf(false) }
    LaunchedEffect(fullscreen) {
        windowState.placement =
            if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "slides",
        state = windowState,
    ) {
        CompositionLocalProvider(
            LocalFullscreenControl provides { enabled -> fullscreen = enabled },
        ) {
            App()
        }
    }
}
