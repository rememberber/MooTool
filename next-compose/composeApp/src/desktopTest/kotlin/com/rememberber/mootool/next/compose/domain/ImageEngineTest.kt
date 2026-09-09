package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.ImageLibraryStore
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImageEngineTest {
    @Test
    fun mapsNamesRangesAndAnchors() {
        assertEquals(ImageSize(600, 400), ImageEngine.scaledDimensions(1200, 800, 0.5f))
        assertEquals(ImageSize(1, 1), ImageEngine.scaledDimensions(3, 3, 0f))
        assertEquals("photo_compressed.jpg", ImageEngine.processedImageName("photo.jpg", "compressed"))
        assertEquals("logo_watermarked.jpg", ImageEngine.processedImageName("logo.png", "watermarked", ImageOutputFormat.Jpeg))
        assertEquals("data:image/png;base64,YWJj", ImageEngine.ensureImageDataUrl("YWJj"))
        assertEquals("data:image/jpeg;base64,YWJj", ImageEngine.ensureImageDataUrl("data:image/jpeg;base64,YWJj"))
        assertEquals(780 to 580, ImageEngine.watermarkAnchor(1000, 600, 200, 40, WatermarkPosition.BottomRight, 20))
        assertEquals(400 to 320, ImageEngine.watermarkAnchor(1000, 600, 200, 40, WatermarkPosition.Center, 20))
        assertEquals(
            ImageCropRect(10, 20, 30, 40),
            ImageEngine.captureRectFromPoints(10, 20, 40, 60, 100, 100)
        )
        val rotated = ImageOrientation.apply(numbered(2, 4), 6)
        assertEquals(4, rotated.width)
        assertEquals(2, rotated.height)
    }

    @Test
    fun compressesWatermarksAndVectorizesWithoutEmbeddedBitmap() {
        val source = numbered(48, 32)
        val compressed = ImageEngine.encodeCompressed(
            source,
            sourceJpeg = false,
            CompressImageOptions(quality = 0.4f, scale = 0.5f, format = ImageOutputFormat.Jpeg)
        )
        val decoded = ImageEngine.decode(compressed)
        assertEquals(24, decoded.width)
        assertEquals(16, decoded.height)
        val marked = ImageEngine.watermark(
            source,
            WatermarkImageOptions("Moo", 1f, "#FF0000", WatermarkPosition.Center, WatermarkFontSize.Large, false)
        )
        assertTrue(marked.getRGB(0, 0) != marked.getRGB(marked.width / 2, marked.height / 2) || hasNonSourcePixel(source, marked))
        val svg = ImageEngine.vectorize(
            twoTone(16, 16),
            ImageVectorizeOptions(ImageSvgPreset.Poster, 4, ImageSvgDetail.Medium, 0)
        )
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("<path"))
        assertTrue(!svg.contains("<image"))
        assertTrue(!svg.contains("data:image/"))
        assertFailsWith<ImageException> {
            ImageEngine.normalizeVectorizeOptions(ImageVectorizeOptions(ImageSvgPreset.Poster, 1, ImageSvgDetail.Medium, 8))
        }
        val bw = ImageEngine.vectorize(
            twoTone(12, 12),
            ImageVectorizeOptions(ImageSvgPreset.Bw, 16, ImageSvgDetail.High, 0)
        )
        assertTrue(bw.contains("<path"))
    }

    @Test
    fun libraryPersistsAndRejectsBadInput() {
        val directory = Files.createTempDirectory("mootool-images-")
        try {
            val store = ImageLibraryStore(directory)
            val file = directory.resolve("source").also { Files.createDirectories(it) }.resolve("Quarterly.PNG")
            val png = ImageEngine.toPng(numbered(8, 6))
            file.writeBytes(png)
            val imported = store.importFiles(listOf(file))
            assertEquals(1, imported.size)
            assertEquals("Quarterly.PNG", imported[0].name)
            val renamed = store.rename("Quarterly.PNG", "shot")
            assertEquals("shot.PNG", renamed.name)
            val read = store.read(renamed.name)
            assertEquals(8, read.width)
            store.delete(listOf(renamed.name))
            assertTrue(store.list().isEmpty())
            assertFailsWith<ImageException> { ImageEngine.ensureImageDataUrl("%%%") }
            assertFailsWith<ImageException> { ImageEngine.watermark(numbered(4, 4), WatermarkImageOptions("  ", 0.5f, "#fff", WatermarkPosition.Center, WatermarkFontSize.Auto, false)) }
        } finally {
            directory.listDirectoryEntries().forEach { child ->
                if (Files.isDirectory(child)) child.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
                Files.deleteIfExists(child)
            }
            Files.deleteIfExists(directory)
        }
    }

    private fun numbered(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, Color(x * 8 % 256, y * 12 % 256, 80, 255).rgb)
            }
        }
        return image
    }

    private fun twoTone(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, if (x < width / 2) Color(220, 40, 20).rgb else Color(20, 40, 210).rgb)
            }
        }
        return image
    }

    private fun hasNonSourcePixel(source: BufferedImage, marked: BufferedImage): Boolean {
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                if (source.getRGB(x, y) != marked.getRGB(x, y)) return true
            }
        }
        return false
    }
}
