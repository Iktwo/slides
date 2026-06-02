@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.iktwo.slides

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

// The browser DnD hook is installed at the document level (once per page).
// Each active composable subscribes via DisposableEffect and receives batches.
private val dropSubscribers = mutableListOf<(List<ByteArray>) -> Unit>()
private var dropInstalled = false

private fun ensureDropInstalled() {
    if (dropInstalled) return
    dropInstalled = true
    installDropBridge { batches ->
        val result = mutableListOf<ByteArray>()
        val outer = batches
        val len = outer.length
        for (i in 0 until len) {
            val inner = outer[i] ?: continue
            val innerLen = inner.length
            val arr = ByteArray(innerLen)
            for (j in 0 until innerLen) {
                arr[j] = (inner[j]?.toInt() ?: 0).toByte()
            }
            result += arr
        }
        if (result.isNotEmpty()) {
            dropSubscribers.toList().forEach { it(result) }
        }
    }
}

@Composable
actual fun ImageDropArea(
    onImagesDropped: (List<ByteArray>) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val latest by rememberUpdatedState(onImagesDropped)
    DisposableEffect(Unit) {
        ensureDropInstalled()
        val handler: (List<ByteArray>) -> Unit = { bytes -> latest(bytes) }
        dropSubscribers += handler
        onDispose { dropSubscribers -= handler }
    }
    Box(modifier = modifier) { content() }
}

@JsFun(
    """
    (onDropped) => {
        const accept = ['jpg','jpeg','png','webp','bmp','gif','heic','heif'];
        document.addEventListener('dragover', (e) => { e.preventDefault(); });
        document.addEventListener('drop', async (e) => {
            e.preventDefault();
            const dt = e.dataTransfer;
            if (!dt) return;
            const items = dt.files;
            if (!items || items.length === 0) return;
            const results = [];
            for (let i = 0; i < items.length; i++) {
                const f = items[i];
                const name = (f.name || '').toLowerCase();
                const dot = name.lastIndexOf('.');
                const ext = dot >= 0 ? name.substring(dot + 1) : '';
                if (accept.indexOf(ext) < 0) continue;
                try {
                    const buf = await f.arrayBuffer();
                    const view = new Uint8Array(buf);
                    const out = new Array(view.length);
                    for (let k = 0; k < view.length; k++) out[k] = view[k];
                    results.push(out);
                } catch (_) { /* skip unreadable files */ }
            }
            if (results.length > 0) onDropped(results);
        });
    }
    """,
)
private external fun installDropBridge(
    onDropped: (JsArray<JsArray<JsNumber>>) -> Unit,
)

@JsFun(
    """
    (enabled) => {
        if (enabled) {
            const el = document.documentElement;
            if (el && el.requestFullscreen) el.requestFullscreen().catch(() => {});
        } else if (document.fullscreenElement && document.exitFullscreen) {
            document.exitFullscreen().catch(() => {});
        }
    }
    """,
)
internal external fun setBrowserFullscreen(enabled: Boolean)

@JsFun(
    """
    () => {
        window.addEventListener('contextmenu', (e) => {
            e.stopImmediatePropagation();
        }, true);
    }
    """,
)
internal external fun releaseContextMenu()

@JsFun(
    """
    () => {
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('sw.js').catch(() => {});
            });
        }
    }
    """,
)
internal external fun registerServiceWorker()

private const val CONFIG_KEY = "slides.config"

@JsFun(
    """
    (key) => {
        try { return window.localStorage.getItem(key); } catch (_) { return null; }
    }
    """,
)
private external fun lsGet(key: String): String?

@JsFun(
    """
    (key, value) => {
        try { window.localStorage.setItem(key, value); } catch (_) {}
    }
    """,
)
private external fun lsSet(key: String, value: String)

actual fun loadConfigString(): String? = lsGet(CONFIG_KEY)
actual fun saveConfigString(value: String) { lsSet(CONFIG_KEY, value) }

@JsFun(
    """
    (onPicked) => {
        const accept = ['jpg','jpeg','png','webp','bmp','gif','heic','heif','svg'];
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = 'image/*';
        input.style.display = 'none';
        document.body.appendChild(input);
        const cleanup = () => { try { document.body.removeChild(input); } catch (_) {} };
        input.addEventListener('change', async () => {
            const results = [];
            const files = input.files;
            if (files) {
                for (let i = 0; i < files.length; i++) {
                    const f = files[i];
                    const nm = (f.name || '').toLowerCase();
                    const dot = nm.lastIndexOf('.');
                    const ext = dot >= 0 ? nm.substring(dot + 1) : '';
                    const typeOk = (f.type || '').startsWith('image/');
                    if (accept.indexOf(ext) < 0 && !typeOk) continue;
                    try {
                        const buf = await f.arrayBuffer();
                        const view = new Uint8Array(buf);
                        const out = new Array(view.length);
                        for (let k = 0; k < view.length; k++) out[k] = view[k];
                        results.push(out);
                    } catch (_) {}
                }
            }
            cleanup();
            onPicked(results);
        });
        input.addEventListener('cancel', () => { cleanup(); onPicked([]); });
        input.click();
    }
    """,
)
private external fun wasmPickImages(onPicked: (JsArray<JsArray<JsNumber>>) -> Unit)

actual fun openImagePicker(onImagesPicked: (List<ByteArray>) -> Unit) {
    wasmPickImages { outer ->
        val result = mutableListOf<ByteArray>()
        val len = outer.length
        for (i in 0 until len) {
            val inner = outer[i] ?: continue
            val innerLen = inner.length
            val arr = ByteArray(innerLen)
            for (j in 0 until innerLen) {
                arr[j] = (inner[j]?.toInt() ?: 0).toByte()
            }
            result += arr
        }
        if (result.isNotEmpty()) onImagesPicked(result)
    }
}

@JsFun(
    """
    (forward, back, exit) => {
        const handler = (e) => {
            const k = e.key;
            if (k === 'ArrowRight' || k === ' ' || e.code === 'Space' || k === 'PageDown') {
                e.preventDefault(); e.stopImmediatePropagation();
                forward();
            } else if (k === 'ArrowLeft' || k === 'PageUp') {
                e.preventDefault(); e.stopImmediatePropagation();
                back();
            } else if (k === 'Escape') {
                e.stopImmediatePropagation();
                exit();
            }
        };
        window.addEventListener('keydown', handler, true);
        window.__slidesKeyHandler = handler;
    }
    """,
)
private external fun wasmInstallKeys(
    forward: () -> Unit,
    back: () -> Unit,
    exit: () -> Unit,
)

@JsFun(
    """
    () => {
        if (window.__slidesKeyHandler) {
            window.removeEventListener('keydown', window.__slidesKeyHandler, true);
            window.__slidesKeyHandler = null;
        }
    }
    """,
)
private external fun wasmUninstallKeys()

actual fun installSlideshowKeys(
    onForward: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
) { wasmInstallKeys(onForward, onBack, onExit) }

actual fun uninstallSlideshowKeys() { wasmUninstallKeys() }
