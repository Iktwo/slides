package com.iktwo.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SlideshowScreen(state: SlideshowState, onExit: () -> Unit) {
    if (state.images.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No images", color = Color.White)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onExit) {
                    Text("Back to Lobby", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
        return
    }

    val focusReq = FocusRequester()
    var slideSeed by remember { mutableStateOf(1L) }
    var currentSlide by remember { mutableStateOf(0) }
    var previousSlideIndex by remember { mutableStateOf<Int?>(null) }
    var showHints by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5000)
        showHints = false
    }

    val slides = remember(
        state.images.size,
        state.mode,
        state.gridCount,
        state.random,
        if (state.random) slideSeed else -1L,
    ) {
        buildSlidePlan(state, slideSeed)
    }

    LaunchedEffect(slideSeed) {
        currentSlide = currentSlide.coerceAtMost(slides.size - 1)
    }

    LaunchedEffect(Unit) {
        focusReq.requestFocus()
    }

    val advance: () -> Unit = {
        if (slides.isNotEmpty()) {
            previousSlideIndex = currentSlide
            if (currentSlide + 1 >= slides.size) {
                slideSeed += 1
                currentSlide = 0
            } else {
                currentSlide += 1
            }
        }
    }
    val back: () -> Unit = {
        previousSlideIndex?.let { idx ->
            currentSlide = idx
        } ?: run {
            currentSlide = (currentSlide - 1).coerceAtLeast(0)
        }
    }
    val advanceLatest by rememberUpdatedState(advance)
    val backLatest by rememberUpdatedState(back)
    val exitLatest by rememberUpdatedState(onExit)
    val slidesLatest by rememberUpdatedState(slides)
    val intervalLatest by rememberUpdatedState(state.intervalMs)
    val repeatLatest by rememberUpdatedState(state.repeat)

    DisposableEffect(Unit) {
        installSlideshowKeys(
            onForward = { advanceLatest() },
            onBack = { backLatest() },
            onExit = { exitLatest() },
        )
        onDispose { uninstallSlideshowKeys() }
    }

    LaunchedEffect(intervalLatest, repeatLatest) {
        while (true) {
            delay(intervalLatest)
            if (slidesLatest.isEmpty()) continue
            if (currentSlide + 1 >= slidesLatest.size) {
                if (repeatLatest) {
                    slideSeed += 1
                    currentSlide = 0
                }
            } else {
                currentSlide += 1
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusReq)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                showHints = false
                when (ev.key) {
                    Key.Escape -> { onExit(); true }
                    Key.DirectionRight, Key.Spacebar, Key.PageDown -> { advance(); true }
                    Key.DirectionLeft, Key.PageUp -> { back(); true }
                    else -> false
                }
            },
    ) {
        if (showHints) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .alpha(0.7f),
            ) {
                HintChip("← →", "Navigate")
                HintChip("Space", "Next")
                HintChip("Esc", "Exit")
            }
        }
        val slide = slides.getOrNull(currentSlide)
        if (slide != null) {
            val layoutSeed =
                if (state.shiftLayout) slideSeed * 1000 + currentSlide
                else slideSeed
            SlideContent(
                images = slide,
                mode = state.mode,
                seed = layoutSeed,
            )
        }
        TextButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) { Text("Exit", color = Color.White) }
    }
}

@Composable
private fun HintChip(key: String, label: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = key,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun buildSlidePlan(state: SlideshowState, seed: Long): List<List<ImageBitmap>> {
    val imgs = state.images.toList()
    if (imgs.isEmpty()) return emptyList()
    val perSlide = when (state.mode) {
        DisplayMode.FitSingle, DisplayMode.FillSingle -> 1
        DisplayMode.PhasedGrid, DisplayMode.PhasedGridZoom ->
            state.gridCount.coerceAtMost(imgs.size).coerceAtLeast(1)
    }
    val order = imgs.indices.toMutableList()
    if (state.random) order.shuffle(Random(seed))
    val n = order.size
    val slideCount =
        if (perSlide >= n) 1
        else ((n + perSlide - 1) / perSlide)
    val slides = mutableListOf<List<ImageBitmap>>()
    for (s in 0 until slideCount) {
        val slice = List(perSlide) { k ->
            imgs[order[(s * perSlide + k) % n]]
        }
        slides += slice
    }
    return slides
}

@Composable
private fun SlideContent(images: List<ImageBitmap>, mode: DisplayMode, seed: Long) {
    when (mode) {
        DisplayMode.FitSingle -> Image(
            bitmap = images.first(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        DisplayMode.FillSingle -> Image(
            bitmap = images.first(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        DisplayMode.PhasedGrid -> PhasedGrid(images = images, zoom = false, seed = seed)
        DisplayMode.PhasedGridZoom -> PhasedGrid(images = images, zoom = true, seed = seed)
    }
}

@Composable
private fun PhasedGrid(images: List<ImageBitmap>, zoom: Boolean, seed: Long) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val containerAspect =
            if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 16f / 9f
        val ordered = remember(images, seed) {
            if (images.size <= 1) images else images.shuffled(Random(seed))
        }
        val layout = remember(ordered, zoom, seed, containerAspect) {
            if (zoom) phasedLayout(ordered.size, seed)
            else masonryLayout(
                   aspects = ordered.map { (it.width.toFloat() / it.height.toFloat()).coerceAtLeast(1f) },
                containerAspect = containerAspect,
            )
        }
        val w = maxWidth
        val h = maxHeight
        for (i in ordered.indices) {
            val r = layout[i]
            val img = ordered[i]
            Box(
                modifier = Modifier
                    .offset(x = w * r.left, y = h * r.top)
                    .size(width = w * r.width, height = h * r.height)
                    .padding(3.dp)
                    .background(Color.Black),
            ) {
                if (zoom) {
                    val focus = LocalFocusDetector.current.focus(img, seed + i)
                    val zoomFactor = rememberRandomZoomFactor(seed + i)
                    ZoomedImage(img, focus, zoomFactor, Modifier.fillMaxSize())
                } else {
                    Image(
                        bitmap = img,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
