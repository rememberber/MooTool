package com.rememberber.mootool.next.compose.domain

import jankovicsandras.imagetracer.ImageTracer
import java.awt.image.BufferedImage

internal object ImageSvg {
    fun convert(image: BufferedImage, options: ImageVectorizeOptions): String {
        ImageEngine.normalizeVectorizeOptions(options)
        ImageEngine.guardPixels(image)
        val tracerOptions = tracerOptions(options)
        val palette = if (options.preset == ImageSvgPreset.Bw) {
            blackAndWhitePalette(hasTransparency(image))
        } else {
            buildPalette(image, options.colorCount)
        }
        val svg = ImageTracer.imageToSVG(image, tracerOptions, palette)
        if (!svg.contains("<svg") || !svg.contains("<path")) {
            throw ImageException("svg-invalid", "Vectorizer returned invalid SVG output")
        }
        if (Regex("<image\\b", RegexOption.IGNORE_CASE).containsMatchIn(svg) || svg.contains("data:image/")) {
            throw ImageException("svg-bitmap", "Vectorizer returned an embedded bitmap instead of vector paths")
        }
        return svg
    }

    private fun tracerOptions(options: ImageVectorizeOptions): HashMap<String, Float> {
        val (lineThreshold, quadraticThreshold, simplifyTolerance, roundCoordinates) = when (options.detail) {
            ImageSvgDetail.Low -> Quad(2f, 2f, 1f, 1)
            ImageSvgDetail.High -> Quad(0.5f, 0.5f, 0f, 3)
            ImageSvgDetail.Medium -> Quad(1f, 1f, 0.25f, 2)
        }
        return hashMapOf(
            "ltres" to lineThreshold,
            "qtres" to quadraticThreshold,
            "pathomit" to options.filterSpeckle.toFloat(),
            "colorsampling" to 0f,
            "numberofcolors" to options.colorCount.toFloat(),
            "mincolorratio" to 0f,
            "colorquantcycles" to if (options.preset == ImageSvgPreset.Photo) 4f else 3f,
            "scale" to 1f,
            "simplifytolerance" to simplifyTolerance,
            "roundcoords" to roundCoordinates.toFloat(),
            "lcpr" to 0f,
            "qcpr" to 0f,
            "desc" to 0f,
            "viewbox" to 1f,
            "blurradius" to if (options.preset == ImageSvgPreset.Photo) 1f else 0f,
            "blurdelta" to 20f
        )
    }

    private fun buildPalette(image: BufferedImage, requestedColors: Int): Array<ByteArray> {
        val histogram = histogram(image)
        val colors = histogram.filterNotNull()
        val transparent = colors.any { it.transparentCount > 0 }
        val opaqueColorLimit = maxOf(1, requestedColors - if (transparent) 1 else 0)
        val opaque = colors.filter { it.count > 0 }
        val boxes = ArrayList<ColorBox>()
        if (opaque.isNotEmpty()) boxes += ColorBox(opaque.toMutableList())
        while (boxes.size < opaqueColorLimit) {
            val box = boxes.filter { it.colors.size > 1 }.maxByOrNull { it.splitScore() } ?: break
            boxes.remove(box)
            boxes += box.split()
        }
        val palette = boxes.sortedBy { it.averageRgb() }.map { it.paletteColor() }.toMutableList()
        if (transparent) palette += tracerColor(0, 0, 0, 0)
        if (palette.isEmpty()) palette += tracerColor(0, 0, 0, 0)
        return palette.toTypedArray()
    }

    private fun histogram(image: BufferedImage): Array<HistogramEntry?> {
        val entries = arrayOfNulls<HistogramEntry>(32 * 32 * 32)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                val alpha = argb ushr 24
                val red = (argb ushr 16) and 0xff
                val green = (argb ushr 8) and 0xff
                val blue = argb and 0xff
                val index = ((red ushr 3) shl 10) or ((green ushr 3) shl 5) or (blue ushr 3)
                val entry = entries[index] ?: HistogramEntry().also { entries[index] = it }
                if (alpha < 16) {
                    entry.transparentCount++
                } else {
                    entry.count++
                    entry.red += red
                    entry.green += green
                    entry.blue += blue
                    entry.alpha += alpha
                }
            }
        }
        return entries
    }

    private fun hasTransparency(image: BufferedImage): Boolean {
        if (!image.colorModel.hasAlpha()) return false
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) ushr 24) < 16) return true
            }
        }
        return false
    }

    private fun blackAndWhitePalette(transparent: Boolean): Array<ByteArray> {
        val palette = ArrayList<ByteArray>(3)
        palette += tracerColor(0, 0, 0, 255)
        palette += tracerColor(255, 255, 255, 255)
        if (transparent) palette += tracerColor(0, 0, 0, 0)
        return palette.toTypedArray()
    }

    private fun tracerColor(red: Int, green: Int, blue: Int, alpha: Int): ByteArray =
        byteArrayOf((red - 128).toByte(), (green - 128).toByte(), (blue - 128).toByte(), (alpha - 128).toByte())

    private data class Quad(val a: Float, val b: Float, val c: Float, val d: Int)

    private class HistogramEntry {
        var count: Long = 0
        var transparentCount: Long = 0
        var red: Long = 0
        var green: Long = 0
        var blue: Long = 0
        var alpha: Long = 0
        fun averageRed() = (red / count).toInt()
        fun averageGreen() = (green / count).toInt()
        fun averageBlue() = (blue / count).toInt()
    }

    private class ColorBox(val colors: MutableList<HistogramEntry>) {
        private val pixelCount = colors.sumOf { it.count }
        private val redRange = range(colors) { it.averageRed() }
        private val greenRange = range(colors) { it.averageGreen() }
        private val blueRange = range(colors) { it.averageBlue() }

        fun splitScore() = pixelCount * maxOf(redRange, greenRange, blueRange)

        fun split(): List<ColorBox> {
            val channel: (HistogramEntry) -> Int = when {
                greenRange >= redRange && greenRange >= blueRange -> { it -> it.averageGreen() }
                blueRange >= redRange && blueRange >= greenRange -> { it -> it.averageBlue() }
                else -> { it -> it.averageRed() }
            }
            colors.sortBy(channel)
            val midpoint = maxOf(1, pixelCount / 2)
            var accumulated = 0L
            var splitAt = 1
            for (i in 0 until colors.size - 1) {
                accumulated += colors[i].count
                splitAt = i + 1
                if (accumulated >= midpoint) break
            }
            return listOf(
                ColorBox(colors.subList(0, splitAt).toMutableList()),
                ColorBox(colors.subList(splitAt, colors.size).toMutableList())
            )
        }

        fun paletteColor(): ByteArray {
            var red = 0L
            var green = 0L
            var blue = 0L
            var alpha = 0L
            colors.forEach {
                red += it.red
                green += it.green
                blue += it.blue
                alpha += it.alpha
            }
            return tracerColor(
                (red / pixelCount).toInt(),
                (green / pixelCount).toInt(),
                (blue / pixelCount).toInt(),
                (alpha / pixelCount).toInt()
            )
        }

        fun averageRgb(): Int {
            val color = paletteColor()
            return ((color[0] + 128) shl 16) or ((color[1] + 128) shl 8) or (color[2] + 128)
        }

        private fun range(colors: List<HistogramEntry>, channel: (HistogramEntry) -> Int): Int {
            var minimum = 255
            var maximum = 0
            colors.forEach {
                val value = channel(it)
                minimum = minOf(minimum, value)
                maximum = maxOf(maximum, value)
            }
            return maximum - minimum
        }
    }
}
