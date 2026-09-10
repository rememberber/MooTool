package com.rememberber.mootool.next.compose.domain

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Locale
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ImageOutputFormat { Auto, Png, Jpeg }
enum class ImageOutputMode { Keep, Overwrite }
enum class WatermarkPosition { BottomRight, BottomLeft, TopRight, TopLeft, Center, Tile }
enum class WatermarkFontSize { Auto, Small, Medium, Large }
enum class ImageSvgPreset { Poster, Photo, Bw }
enum class ImageSvgDetail { Low, Medium, High }

data class ImageCropRect(val x: Int, val y: Int, val width: Int, val height: Int)
data class ImageSize(val width: Int, val height: Int)

data class CompressImageOptions(
    val quality: Float,
    val scale: Float,
    val format: ImageOutputFormat
)

data class WatermarkImageOptions(
    val text: String,
    val opacity: Float,
    val color: String,
    val position: WatermarkPosition,
    val fontSize: WatermarkFontSize,
    val diagonal: Boolean
)

data class ImageVectorizeOptions(
    val preset: ImageSvgPreset,
    val colorCount: Int,
    val detail: ImageSvgDetail,
    val filterSpeckle: Int
) {
    companion object {
        fun defaults() = ImageVectorizeOptions(ImageSvgPreset.Poster, 16, ImageSvgDetail.Medium, 8)
    }
}

class ImageException(val code: String, message: String) : Exception(message)

object ImageEngine {
    const val MAX_PIXELS = 16_000_000L
    const val MAX_BATCH = 20
    private val dataUrlPattern = Regex("^data:image/[\\w.+-]+;base64,", RegexOption.IGNORE_CASE)
    private val rawBase64Pattern = Regex("^[A-Za-z0-9+/=\\s]+$")

    fun scaledDimensions(width: Int, height: Int, scale: Float): ImageSize {
        val normalized = scale.coerceIn(0.1f, 1f)
        return ImageSize(
            width = max(1, (width * normalized).roundToInt()),
            height = max(1, (height * normalized).roundToInt())
        )
    }

    fun processedImageName(name: String, suffix: String, format: ImageOutputFormat = ImageOutputFormat.Auto): String {
        val extensionIndex = name.lastIndexOf('.')
        val base = if (extensionIndex > 0) name.substring(0, extensionIndex) else name
        val current = if (extensionIndex > 0) name.substring(extensionIndex + 1).lowercase() else "png"
        val extension = when (format) {
            ImageOutputFormat.Jpeg -> "jpg"
            ImageOutputFormat.Png -> "png"
            ImageOutputFormat.Auto -> if (current == "jpg" || current == "jpeg") "jpg" else "png"
        }
        return "${base}_${suffix}.$extension"
    }

    fun overwriteName(name: String, format: ImageOutputFormat): String {
        if (format == ImageOutputFormat.Auto) return name
        val base = name.replace(Regex("\\.[^.]+$"), "")
        return "$base.${if (format == ImageOutputFormat.Jpeg) "jpg" else "png"}"
    }

    fun watermarkAnchor(
        width: Int,
        height: Int,
        textWidth: Int,
        textHeight: Int,
        position: WatermarkPosition,
        margin: Int
    ): Pair<Int, Int> {
        return when (position) {
            WatermarkPosition.TopLeft -> margin to (margin + textHeight)
            WatermarkPosition.TopRight -> (width - textWidth - margin) to (margin + textHeight)
            WatermarkPosition.BottomLeft -> margin to (height - margin)
            WatermarkPosition.Center -> ((width - textWidth) / 2) to ((height + textHeight) / 2)
            WatermarkPosition.BottomRight, WatermarkPosition.Tile ->
                (width - textWidth - margin) to (height - margin)
        }
    }

    fun ensureImageDataUrl(value: String): String {
        val trimmed = value.trim()
        if (dataUrlPattern.containsMatchIn(trimmed)) return trimmed
        if (rawBase64Pattern.matches(trimmed) && trimmed.isNotEmpty()) {
            return "data:image/png;base64,${trimmed.replace("\\s+".toRegex(), "")}"
        }
        throw ImageException("invalid-base64", "Invalid image Base64")
    }

    fun decodeDataUrl(value: String): BufferedImage {
        val dataUrl = ensureImageDataUrl(value)
        val comma = dataUrl.indexOf(',')
        if (comma < 0) throw ImageException("invalid-base64", "Invalid image Base64")
        val bytes = runCatching { Base64.getDecoder().decode(dataUrl.substring(comma + 1)) }
            .getOrElse { throw ImageException("invalid-base64", "Invalid image Base64") }
        return decode(bytes)
    }

    fun decode(bytes: ByteArray): BufferedImage {
        val image = ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw ImageException("invalid-image", "Unable to read image")
        val oriented = ImageOrientation.apply(image, ImageOrientation.jpegOrientation(bytes))
        guardPixels(oriented)
        return oriented
    }

    fun decodeFile(bytes: ByteArray): BufferedImage = decode(bytes)

    fun toPng(image: BufferedImage): ByteArray = encode(image, ImageOutputFormat.Png, 0.92f)

    fun toDataUrl(image: BufferedImage, format: ImageOutputFormat = ImageOutputFormat.Png): String {
        val mime = if (format == ImageOutputFormat.Jpeg) "image/jpeg" else "image/png"
        return "data:$mime;base64,${Base64.getEncoder().encodeToString(encode(image, format, 0.92f))}"
    }

    fun compress(image: BufferedImage, sourceJpeg: Boolean, options: CompressImageOptions): BufferedImage {
        val size = scaledDimensions(image.width, image.height, options.scale)
        val format = resolveFormat(options.format, sourceJpeg)
        val type = if (format == ImageOutputFormat.Jpeg) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        val output = BufferedImage(size.width, size.height, type)
        val graphics = output.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            if (format == ImageOutputFormat.Jpeg) {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, size.width, size.height)
            }
            graphics.drawImage(image, 0, 0, size.width, size.height, null)
        } finally {
            graphics.dispose()
        }
        return decode(encode(output, format, options.quality.coerceIn(0.01f, 1f)))
    }

    fun encodeCompressed(image: BufferedImage, sourceJpeg: Boolean, options: CompressImageOptions): ByteArray {
        val size = scaledDimensions(image.width, image.height, options.scale)
        val format = resolveFormat(options.format, sourceJpeg)
        val type = if (format == ImageOutputFormat.Jpeg) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        val output = BufferedImage(size.width, size.height, type)
        val graphics = output.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            if (format == ImageOutputFormat.Jpeg) {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, size.width, size.height)
            }
            graphics.drawImage(image, 0, 0, size.width, size.height, null)
        } finally {
            graphics.dispose()
        }
        return encode(output, format, options.quality.coerceIn(0.01f, 1f))
    }

    fun watermark(image: BufferedImage, options: WatermarkImageOptions): BufferedImage {
        val text = options.text.trim()
        if (text.isEmpty()) throw ImageException("watermark-text", "Watermark text is required")
        val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()
        try {
            graphics.drawImage(image, 0, 0, null)
            val fontSize = resolveWatermarkFontSize(image.width, image.height, options.fontSize)
            graphics.font = Font(Font.SANS_SERIF, Font.BOLD, fontSize)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val metrics = graphics.fontMetrics
            val textWidth = metrics.stringWidth(text)
            val textHeight = (fontSize * 1.2f).roundToInt()
            val margin = max(8, (min(image.width, image.height) * 0.02f).roundToInt())
            graphics.color = colorWithAlpha(options.color, options.opacity.coerceIn(0.01f, 1f))
            graphics.composite = AlphaComposite.SrcOver
            if (options.position == WatermarkPosition.Tile) {
                val stepX = textWidth + margin * 3
                val stepY = textHeight + margin * 3
                var y = -image.height
                while (y < image.height * 2) {
                    var x = -image.width
                    while (x < image.width * 2) {
                        drawText(graphics, text, x, y, options.diagonal)
                        x += stepX
                    }
                    y += stepY
                }
            } else {
                val (x, y) = watermarkAnchor(image.width, image.height, textWidth, textHeight, options.position, margin)
                drawText(graphics, text, x, y, options.diagonal)
            }
        } finally {
            graphics.dispose()
        }
        return output
    }

    fun crop(image: BufferedImage, rect: ImageCropRect): BufferedImage {
        val x = max(0, rect.x)
        val y = max(0, rect.y)
        val width = min(image.width - x, max(1, rect.width))
        val height = min(image.height - y, max(1, rect.height))
        if (x >= image.width || y >= image.height || width < 1 || height < 1) {
            throw ImageException("crop", "Crop rectangle is outside the image")
        }
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()
        try {
            graphics.drawImage(image, 0, 0, width, height, x, y, x + width, y + height, null)
        } finally {
            graphics.dispose()
        }
        return output
    }

    fun captureRectFromPoints(startX: Int, startY: Int, endX: Int, endY: Int, width: Int, height: Int): ImageCropRect {
        val widthLimit = max(1, width)
        val heightLimit = max(1, height)
        val left = min(startX, endX).coerceIn(0, widthLimit)
        val top = min(startY, endY).coerceIn(0, heightLimit)
        val right = max(startX, endX).coerceIn(0, widthLimit)
        val bottom = max(startY, endY).coerceIn(0, heightLimit)
        return clampCaptureRect(
            ImageCropRect(left, top, max(1, right - left), max(1, bottom - top)),
            widthLimit,
            heightLimit
        )
    }

    fun clampCaptureRect(rect: ImageCropRect, width: Int, height: Int): ImageCropRect {
        val widthLimit = max(1, width)
        val heightLimit = max(1, height)
        val x = rect.x.coerceIn(0, widthLimit - 1)
        val y = rect.y.coerceIn(0, heightLimit - 1)
        return ImageCropRect(
            x = x,
            y = y,
            width = rect.width.coerceIn(1, widthLimit - x),
            height = rect.height.coerceIn(1, heightLimit - y)
        )
    }

    fun vectorize(image: BufferedImage, options: ImageVectorizeOptions): String = ImageSvg.convert(image, options)

    fun normalizeVectorizeOptions(options: ImageVectorizeOptions): ImageVectorizeOptions {
        if (options.colorCount !in 2..64) throw ImageException("svg-colors", "Color count must be between 2 and 64")
        if (options.filterSpeckle !in 0..128) throw ImageException("svg-speckle", "Speckle filter must be between 0 and 128")
        return options
    }

    fun resolveFormat(format: ImageOutputFormat, sourceJpeg: Boolean): ImageOutputFormat = when (format) {
        ImageOutputFormat.Auto -> if (sourceJpeg) ImageOutputFormat.Jpeg else ImageOutputFormat.Png
        else -> format
    }

    fun isJpegName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext == "jpg" || ext == "jpeg"
    }

    fun formatBytes(bytes: Long): String {
        val value = bytes.coerceAtLeast(0)
        return when {
            value < 1024 -> "$value B"
            value < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", value / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024.0))
        }
    }

    fun timestampName(prefix: String): String {
        val stamp = java.time.Instant.now().toString().replace(":", "-").replace(".", "-").take(19)
        return "$prefix-$stamp.png"
    }

    fun guardPixels(image: BufferedImage) {
        if (image.width.toLong() * image.height.toLong() > MAX_PIXELS) {
            throw ImageException("too-large", "Image exceeds the 16 megapixel limit")
        }
    }

    fun encode(image: BufferedImage, format: ImageOutputFormat, quality: Float): ByteArray {
        val output = ByteArrayOutputStream()
        if (format == ImageOutputFormat.Jpeg) {
            val rgb = if (image.type == BufferedImage.TYPE_INT_RGB) image else flattenWhite(image)
            val writers = ImageIO.getImageWritersByFormatName("jpeg")
            if (!writers.hasNext()) throw ImageException("encode", "JPEG writer is unavailable")
            val writer = writers.next()
            writer.output = ImageIO.createImageOutputStream(output)
            val param = writer.defaultWriteParam
            if (param.canWriteCompressed()) {
                param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                param.compressionQuality = quality.coerceIn(0.01f, 1f)
            }
            writer.write(null, IIOImage(rgb, null, null), param)
            writer.dispose()
        } else {
            if (!ImageIO.write(image, "png", output)) throw ImageException("encode", "PNG writer is unavailable")
        }
        return output.toByteArray()
    }

    private fun flattenWhite(image: BufferedImage): BufferedImage {
        val rgb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val graphics = rgb.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, image.width, image.height)
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return rgb
    }

    private fun resolveWatermarkFontSize(width: Int, height: Int, mode: WatermarkFontSize): Int {
        val base = (min(width, height) * 0.05f).roundToInt()
        return when (mode) {
            WatermarkFontSize.Small -> max(16, min(base, 28))
            WatermarkFontSize.Medium -> max(24, min(base + 8, 42))
            WatermarkFontSize.Large -> max(32, min(base + 16, 64))
            WatermarkFontSize.Auto -> max(18, min(base, 48))
        }
    }

    private fun colorWithAlpha(color: String, opacity: Float): Color {
        val hex = color.removePrefix("#")
        if (!hex.matches(Regex("^[0-9a-fA-F]{6}$"))) return Color(1f, 1f, 1f, opacity)
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        return Color(r / 255f, g / 255f, b / 255f, opacity)
    }

    private fun drawText(graphics: Graphics2D, text: String, x: Int, y: Int, diagonal: Boolean) {
        val transform = graphics.transform
        if (diagonal) {
            graphics.translate(x.toDouble(), y.toDouble())
            graphics.rotate(-Math.PI / 4)
            graphics.translate(-x.toDouble(), -y.toDouble())
        }
        graphics.drawString(text, x, y)
        graphics.transform = transform
    }
}
