package com.iktwo.slides

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.random.Random

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
