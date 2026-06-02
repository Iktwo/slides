package com.iktwo.slides

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    releaseContextMenu()
    registerServiceWorker()
    ComposeViewport {
        CompositionLocalProvider(
            LocalFullscreenControl provides { enabled -> setBrowserFullscreen(enabled) },
        ) {
            App()
        }
    }
}
