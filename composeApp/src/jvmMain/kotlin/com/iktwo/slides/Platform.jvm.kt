package com.iktwo.slides

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.awt.datatransfer.DataFlavor
import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

private val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif",
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun ImageDropArea(
    onImagesDropped: (List<ByteArray>) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val onImagesDroppedRef = rememberUpdatedState(onImagesDropped)
    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = try {
                    event.awtTransferable
                } catch (_: Throwable) {
                    return false
                }
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                @Suppress("UNCHECKED_CAST")
                val files = try {
                    transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                } catch (_: Throwable) {
                    return false
                }
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        files.flatMap { collectImages(it) }
                            .map { it.readBytes() }
                    }
                    if (bytes.isNotEmpty()) onImagesDroppedRef.value(bytes)
                }
                return true
            }
        }
    }
    Box(
        modifier = modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = target,
        ),
    ) {
        content()
    }
}

private fun collectImages(file: File): List<File> =
    if (file.isDirectory) file.walkTopDown().filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }.toList()
    else if (file.extension.lowercase() in IMAGE_EXTENSIONS) listOf(file)
    else emptyList()

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

private val configFile: File by lazy {
    val dir = File(System.getProperty("user.home"), ".slides")
    dir.mkdirs()
    File(dir, "config.txt")
}

actual fun loadConfigString(): String? = runCatching {
    if (configFile.isFile) configFile.readText() else null
}.getOrNull()

actual fun saveConfigString(value: String) {
    runCatching { configFile.writeText(value) }
}

actual fun openImagePicker(onImagesPicked: (List<ByteArray>) -> Unit) {
    javax.swing.SwingUtilities.invokeLater {
        val chooser = javax.swing.JFileChooser().apply {
            isMultiSelectionEnabled = true
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "Images",
                *IMAGE_EXTENSIONS.toTypedArray(),
            )
        }
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val selected = chooser.selectedFiles.toList()
            CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
                val bytes = withContext(Dispatchers.IO) {
                    selected.flatMap { collectImages(it) }
                        .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
                        .map { it.readBytes() }
                }
                if (bytes.isNotEmpty()) {
                    onImagesPicked(bytes)
                }
            }
        }
    }
}

// JVM uses Compose focus for slideshow keys; no window-level handler needed.
actual fun installSlideshowKeys(
    onForward: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
) { /* no-op */ }

actual fun uninstallSlideshowKeys() { /* no-op */ }
