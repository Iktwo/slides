package com.iktwo.slides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

val LocalFullscreenControl = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

val LocalFocusDetector = staticCompositionLocalOf<FocusDetector> { RandomFocusDetector }

@Composable
expect fun ImageDropArea(
    onImagesDropped: (List<ByteArray>) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
)

expect fun decodeImage(bytes: ByteArray): ImageBitmap?

expect fun loadConfigString(): String?
expect fun saveConfigString(value: String)

expect fun openImagePicker(onImagesPicked: (List<ByteArray>) -> Unit)

expect fun installSlideshowKeys(
    onForward: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
)
expect fun uninstallSlideshowKeys()
