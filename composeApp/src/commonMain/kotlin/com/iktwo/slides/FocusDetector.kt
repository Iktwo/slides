package com.iktwo.slides

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

val MAX_ZOOM_FACTOR = 1.7f

fun interface FocusDetector {
    // Returns focal point in normalized 0..1 image coordinates.
    fun focus(image: ImageBitmap, seed: Long): Offset
}

object RandomFocusDetector : FocusDetector {
    override fun focus(image: ImageBitmap, seed: Long): Offset {
        val rng = Random(seed xor (image.width * 31L + image.height))
        return Offset(0.25f + rng.nextFloat() * 0.5f, 0.25f + rng.nextFloat() * 0.5f)
    }
}

// Swap this for a feature-based detector (face/saliency) later.
var focusDetector: FocusDetector = RandomFocusDetector

@Composable
internal fun ZoomedImage(
    image: ImageBitmap,
    focus: Offset,
    zoomFactor: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val iw = image.width.toFloat()
        val ih = image.height.toFloat()
        if (size.width <= 0f || size.height <= 0f || iw <= 0f || ih <= 0f) return@Canvas
        val canvasAspect = size.width / size.height
        val imgAspect = iw / ih
        val fillScale = if (imgAspect > canvasAspect) size.height / ih else size.width / iw
        val scale = fillScale * zoomFactor.coerceIn(1f, MAX_ZOOM_FACTOR)
        val scaledW = iw * scale
        val scaledH = ih * scale
        val focusX = focus.x * scaledW
        val focusY = focus.y * scaledH
        val txMin = (size.width - scaledW).coerceAtMost(0f)
        val tyMin = (size.height - scaledH).coerceAtMost(0f)
        val tx = (size.width / 2f - focusX).coerceIn(txMin, 0f)
        val ty = (size.height / 2f - focusY).coerceIn(tyMin, 0f)
        clipRect {
            translate(tx, ty) {
                scale(scale, scale, pivot = Offset.Zero) {
                    drawImage(image)
                }
            }
        }
    }
}

internal fun randomZoomFactor(seed: Long): Float {
    val rng = Random(seed)
    return 1f + rng.nextFloat() * (MAX_ZOOM_FACTOR - 1f)
}

@Composable
internal fun rememberRandomZoomFactor(seed: Long): Float {
    return remember(seed) { randomZoomFactor(seed) }
}
