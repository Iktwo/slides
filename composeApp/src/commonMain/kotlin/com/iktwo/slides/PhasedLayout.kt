package com.iktwo.slides

import androidx.compose.ui.geometry.Rect
import kotlin.random.Random

// Binary-partition the unit rect until there are `count` cells.
// Result: normalized rects (0..1) that tile the viewport.
fun phasedLayout(count: Int, seed: Long): List<Rect> {
    if (count <= 0) return emptyList()
    val rng = Random(seed)
    val rects = mutableListOf(Rect(0f, 0f, 1f, 1f))
    while (rects.size < count) {
        var idx = 0
        var bestArea = -1f
        for (i in rects.indices) {
            val a = rects[i].area
            if (a > bestArea) { bestArea = a; idx = i }
        }
        val r = rects.removeAt(idx)
        val splitHorizontal = when {
            r.width / r.height > 1.6f -> true
            r.height / r.width > 1.6f -> false
            else -> rng.nextBoolean()
        }
        val ratio = 0.35f + rng.nextFloat() * 0.3f
        if (splitHorizontal) {
            val mid = r.left + r.width * ratio
            rects += Rect(r.left, r.top, mid, r.bottom)
            rects += Rect(mid, r.top, r.right, r.bottom)
        } else {
            val mid = r.top + r.height * ratio
            rects += Rect(r.left, r.top, r.right, mid)
            rects += Rect(r.left, mid, r.right, r.bottom)
        }
    }
    return rects
}

private val Rect.area: Float get() = width * height

// Masonry (Pinterest-style) layout. Columns of equal width. Each image in its column
// gets width = columnWidth and height = columnWidth / aspect — aspect preserved exactly.
// Greedy-places each image in the currently-shortest column to balance column heights.
// Tries several column counts; picks the one with best coverage + balance.
// Result uniform-scaled to fit container; any residual is letterboxed.
fun masonryLayout(aspects: List<Float>, containerAspect: Float): List<Rect> {
    val n = aspects.size
    if (n == 0) return emptyList()
    if (n == 1) return listOf(Rect(0f, 0f, 1f, 1f))

    val containerW = 1f
    val containerH = 1f / containerAspect.coerceAtLeast(0.01f)
    val safeAspects = aspects.map { it.coerceAtLeast(0.05f) }

    var best: List<Rect>? = null
    var bestScore = Float.MAX_VALUE
    val maxColumns = kotlin.math.min(n, 8)
    for (c in 1..maxColumns) {
        val layout = buildMasonry(safeAspects, c, containerW, containerH)
        if (best == null || layout.second < bestScore) {
            bestScore = layout.second
            best = layout.first
        }
    }
    return best!!
}

private fun buildMasonry(
    aspects: List<Float>,
    columns: Int,
    containerW: Float,
    containerH: Float,
): Pair<List<Rect>, Float> {
    val n = aspects.size
    val colWidth = containerW / columns
    val colHeights = FloatArray(columns)
    val colOf = IntArray(n)
    val yOf = FloatArray(n)

    for (i in 0 until n) {
        var pick = 0
        for (c in 1 until columns) {
            if (colHeights[c] < colHeights[pick]) pick = c
        }
        colOf[i] = pick
        yOf[i] = colHeights[pick]
        colHeights[pick] += colWidth / aspects[i]
    }

    var maxH = 0f
    var minH = Float.MAX_VALUE
    for (h in colHeights) {
        if (h > maxH) maxH = h
        if (h < minH) minH = h
    }
    val scale = kotlin.math.min(1f, containerH / maxH)
    val scaledW = containerW * scale
    val scaledH = maxH * scale
    val offX = (containerW - scaledW) / 2f
    val offY = (containerH - scaledH) / 2f

    val rects = ArrayList<Rect>(n)
    for (i in 0 until n) {
        val c = colOf[i]
        val h = colWidth / aspects[i]
        val rx = c * colWidth * scale + offX
        val ry = yOf[i] * scale + offY
        val rw = colWidth * scale
        val rh = h * scale
        rects += Rect(
            rx / containerW,
            ry / containerH,
            (rx + rw) / containerW,
            (ry + rh) / containerH,
        )
    }

    val coverage = scaledW * scaledH / (containerW * containerH)
    val imbalance = if (minH > 0f) (maxH / minH - 1f) else 10f
    val score = -coverage + 0.15f * imbalance
    return rects to score
}

