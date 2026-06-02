package com.iktwo.slides

private const val KEY_MODE = "mode"
private const val KEY_GRID = "gridCount"
private const val KEY_RANDOM = "random"
private const val KEY_REPEAT = "repeat"
private const val KEY_SHIFT = "shiftLayout"
private const val KEY_INTERVAL = "intervalMs"

fun SlideshowState.serializeConfig(): String = buildString {
    append(KEY_MODE).append('=').append(mode.name).append('\n')
    append(KEY_GRID).append('=').append(gridCount).append('\n')
    append(KEY_RANDOM).append('=').append(random).append('\n')
    append(KEY_REPEAT).append('=').append(repeat).append('\n')
    append(KEY_SHIFT).append('=').append(shiftLayout).append('\n')
    append(KEY_INTERVAL).append('=').append(intervalMs)
}

fun SlideshowState.applyConfig(raw: String?) {
    if (raw.isNullOrBlank()) return
    val map = raw.lineSequence()
        .mapNotNull {
            val i = it.indexOf('=')
            if (i <= 0) null else it.substring(0, i).trim() to it.substring(i + 1).trim()
        }
        .toMap()
    map[KEY_MODE]?.let { name ->
        DisplayMode.entries.firstOrNull { it.name == name }?.let { mode = it }
    }
    map[KEY_GRID]?.toIntOrNull()?.let { gridCount = it.coerceIn(1, 12) }
    map[KEY_RANDOM]?.let { random = it == "true" }
    map[KEY_REPEAT]?.let { repeat = it == "true" }
    map[KEY_SHIFT]?.let { shiftLayout = it == "true" }
    map[KEY_INTERVAL]?.toLongOrNull()?.let {
        intervalMs = it.coerceIn(16L, 24L * 60L * 60L * 1000L)
    }
}
