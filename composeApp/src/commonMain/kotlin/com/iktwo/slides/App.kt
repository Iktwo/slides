package com.iktwo.slides

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = OliveDarkColorScheme) {
        val state = rememberSlideshowState()
        val setFullscreen = LocalFullscreenControl.current
        LaunchedEffect(state.started) { setFullscreen(state.started) }
        CompositionLocalProvider(LocalFocusDetector provides focusDetector) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (state.started) {
                    SlideshowScreen(state) { state.started = false }
                } else {
                    LobbyScreen(state)
                }
            }
        }
    }
}
