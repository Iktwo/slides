package com.iktwo.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREVIEW_SEED = 42L
private const val PREVIEW_SHIFT_MIN_MS = 150L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
    ) { content() }
}

@Composable
fun LobbyScreen(state: SlideshowState) {
    val scope = rememberCoroutineScope()
    var loadingCount by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loadError) {
        if (loadError != null) {
            delay(4000)
            loadError = null
        }
    }

    val addBytes: (List<ByteArray>) -> Unit = { bytes ->
        scope.launch {
            loadingCount += bytes.size
            try {
                val bmps = withContext(Dispatchers.Default) {
                    val decoded = bytes.mapIndexed { i, b ->
                        val result = decodeImage(b)
                        if (result == null) {
                            withContext(Dispatchers.Main) {
                                loadError = "Failed to decode image #$i"
                            }
                        }
                        result
                    }.filterNotNull()
                    decoded
                }
                state.images.addAll(bmps)
            } finally {
                loadingCount -= bytes.size
            }
        }
    }
    ImageDropArea(
        onImagesDropped = addBytes,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Slides",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Drop images, pick a mode, Start.",
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewPane(state, modifier = Modifier.weight(1f).fillMaxHeight())
                ImagesPane(state, modifier = Modifier.weight(1f).fillMaxHeight())
            }

            Spacer(Modifier.height(16.dp))
            ConfigPanel(state)
            if (loadingCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "Loading $loadingCount image${if (loadingCount > 1) "s" else ""}...",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (loadError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = loadError!!,
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tip("Pick image files from your computer") {
                    OutlinedButton(
                        onClick = { openImagePicker(addBytes) },
                    ) { Text("Pick images") }
                }
                Spacer(Modifier.weight(1f))
                Tip("Enter fullscreen and begin slideshow") {
                    Button(
                        onClick = { state.started = true },
                        enabled = state.images.isNotEmpty(),
                    ) { Text("Start") }
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(state: SlideshowState, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                text = "Preview",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
            ) {
                PreviewContent(state)
            }
        }
    }
}

@Composable
private fun PreviewContent(state: SlideshowState) {
    val slotCount = when (state.mode) {
        DisplayMode.FitSingle, DisplayMode.FillSingle -> 1
        DisplayMode.PhasedGrid, DisplayMode.PhasedGridZoom ->
            state.gridCount.coerceAtLeast(1)
    }
    val displayCount =
        if (state.images.isEmpty()) slotCount
        else slotCount.coerceAtMost(state.images.size).coerceAtLeast(1)

    val sampleImages = remember(state.images, displayCount) {
        if (state.images.isEmpty()) emptyList()
        else state.images.take(displayCount)
    }

    val isGrid = state.mode == DisplayMode.PhasedGrid || state.mode == DisplayMode.PhasedGridZoom
    var shiftTick by remember { mutableStateOf(0) }
    LaunchedEffect(state.shiftLayout, isGrid, state.intervalMs) {
        if (state.shiftLayout && isGrid) {
            val delayMs = state.intervalMs.coerceAtLeast(PREVIEW_SHIFT_MIN_MS)
            while (true) {
                delay(delayMs)
                shiftTick++
            }
        } else {
            shiftTick = 0
        }
    }
    val previewSeed = PREVIEW_SEED + if (state.shiftLayout && isGrid) shiftTick.toLong() else 0L

    when (state.mode) {
        DisplayMode.FitSingle -> SingleSlot(sampleImages.firstOrNull(), fit = true)
        DisplayMode.FillSingle -> SingleSlot(sampleImages.firstOrNull(), fit = false)
        DisplayMode.PhasedGrid -> PreviewGrid(sampleImages, displayCount, zoom = false, seed = previewSeed)
        DisplayMode.PhasedGridZoom -> PreviewGrid(sampleImages, displayCount, zoom = true, seed = previewSeed)
    }
}

@Composable
private fun SingleSlot(image: ImageBitmap?, fit: Boolean) {
    if (image == null) {
        Placeholder(index = 1, modifier = Modifier.fillMaxSize())
    } else {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PreviewGrid(images: List<ImageBitmap>, count: Int, zoom: Boolean, seed: Long) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val containerAspect =
            if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 16f / 9f
        val ordered = remember(images, seed) {
            if (images.size <= 1) images else images.shuffled(kotlin.random.Random(seed))
        }
        val layout = remember(ordered, count, zoom, seed, containerAspect) {
            when {
                zoom -> phasedLayout(count, seed)
                ordered.isEmpty() -> phasedLayout(count, seed)
                else -> masonryLayout(
                    aspects = ordered.map {
                        (it.width.toFloat() / it.height.toFloat()).coerceAtLeast(1f)
                    },
                    containerAspect = containerAspect,
                )
            }
        }
        val w = maxWidth
        val h = maxHeight
        for (i in 0 until count) {
            val r = layout[i]
            val img = ordered.getOrNull(i)
            Box(
                modifier = Modifier
                    .offset(x = w * r.left, y = h * r.top)
                    .size(width = w * r.width, height = h * r.height)
                    .padding(3.dp),
            ) {
                if (img == null) {
                    Placeholder(index = i + 1, modifier = Modifier.fillMaxSize())
                } else if (zoom) {
                    val zf = rememberRandomZoomFactor(seed + i)
                    ZoomedImage(img, LocalFocusDetector.current.focus(img, seed + i), zf, Modifier.fillMaxSize())
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

@Composable
private fun Placeholder(index: Int, modifier: Modifier) {
    val hue = (index * 53) % 360
    val color = Color.hsv(hue.toFloat(), 0.35f, 0.55f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$index",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun Thumbnail(image: ImageBitmap, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.DarkGray),
    ) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(48.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagesPane(state: SlideshowState, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (state.images.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Drop images here",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.images, key = { it.width.toLong() * 1_000_000 + it.height }) { img ->
                        Thumbnail(image = img, onRemove = { state.images.remove(img) })
                    }
                }
            }
            if (state.images.isNotEmpty()) {
                Tip("Remove all dropped images") {
                    TextButton(
                        onClick = { state.images.clear() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                    ) { Text("Clear") }
                }
            }
            Text(
                text = "${state.images.size} images",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigPanel(state: SlideshowState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DisplayMode.entries.forEach { m ->
                Tip(m.description) {
                    FilterChip(
                        selected = state.mode == m,
                        onClick = { state.mode = m },
                        label = { Text(m.label) },
                    )
                }
            }
        }
        if (state.mode == DisplayMode.PhasedGrid || state.mode == DisplayMode.PhasedGridZoom) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column {
                    Text(
                        text = "Images per slide: ${state.gridCount}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Slider(
                        value = state.gridCount.toFloat(),
                        onValueChange = { state.gridCount = it.toInt() },
                        valueRange = 1f..12f,
                        steps = 10,
                        modifier = Modifier.width(200.dp),
                    )
                }
                Tip("Re-shuffle the tile layout on every slide transition. Off = layout stays fixed.") {
                    FilterChip(
                        selected = state.shiftLayout,
                        onClick = { state.shiftLayout = !state.shiftLayout },
                        label = { Text("Shift layout") },
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tip("Shuffle image order before each pass. Off = keep drop order.") {
                FilterChip(
                    selected = state.random,
                    onClick = { state.random = !state.random },
                    label = { Text("Random") },
                )
            }
            Tip("After showing every image, start over from the beginning. Off = stop at the last slide.") {
                FilterChip(
                    selected = state.repeat,
                    onClick = { state.repeat = !state.repeat },
                    label = { Text("Repeat") },
                )
            }
            Tip("Time between slides in auto-advance mode") {
                IntervalInput(state)
            }
        }
    }
}

private enum class TimeUnitOption(val label: String, val toMs: Long) {
    Ms("ms", 1L),
    S("s", 1_000L),
    M("m", 60_000L),
    Hr("hr", 3_600_000L),
}

private const val MIN_INTERVAL_MS = 16L
private const val MAX_INTERVAL_MS = 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalInput(state: SlideshowState) {
    var unit by remember { mutableStateOf(pickBestUnit(state.intervalMs)) }
    var text by remember { mutableStateOf(intervalToDisplay(state.intervalMs, unit)) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(unit) { text = intervalToDisplay(state.intervalMs, unit) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Interval", color = Color.White)
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw
                val parsed = parseInterval(raw, unit)
                if (parsed == null || parsed !in MIN_INTERVAL_MS..MAX_INTERVAL_MS) {
                    error = true
                } else {
                    error = false
                    state.intervalMs = parsed
                }
            },
            isError = error,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(110.dp),
        )
        SingleChoiceSegmentedButtonRow {
            TimeUnitOption.entries.forEachIndexed { i, u ->
                SegmentedButton(
                    selected = unit == u,
                    onClick = { unit = u },
                    shape = SegmentedButtonDefaults.itemShape(i, TimeUnitOption.entries.size),
                ) { Text(u.label) }
            }
        }
    }
}

private fun pickBestUnit(ms: Long): TimeUnitOption = when {
    ms >= TimeUnitOption.Hr.toMs && ms % TimeUnitOption.Hr.toMs == 0L -> TimeUnitOption.Hr
    ms >= TimeUnitOption.M.toMs && ms % TimeUnitOption.M.toMs == 0L -> TimeUnitOption.M
    ms >= TimeUnitOption.S.toMs && ms % TimeUnitOption.S.toMs == 0L -> TimeUnitOption.S
    else -> TimeUnitOption.Ms
}

private fun intervalToDisplay(ms: Long, unit: TimeUnitOption): String {
    if (unit == TimeUnitOption.Ms) return ms.toString()
    val whole = ms / unit.toMs
    val rem = ms % unit.toMs
    if (rem == 0L) return whole.toString()
    val frac3 = (rem * 1000L + unit.toMs / 2L) / unit.toMs
    val digits = frac3.toString().padStart(3, '0').trimEnd('0')
    return if (digits.isEmpty()) whole.toString() else "$whole.$digits"
}

private fun parseInterval(text: String, unit: TimeUnitOption): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val value = trimmed.toDoubleOrNull() ?: return null
    if (value < 0.0 || !value.isFinite()) return null
    return (value * unit.toMs).toLong()
}

