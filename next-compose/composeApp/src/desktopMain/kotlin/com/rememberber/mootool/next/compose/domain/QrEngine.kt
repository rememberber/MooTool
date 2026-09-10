package com.rememberber.mootool.next.compose.domain

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.WriterException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

enum class QrErrorCorrection { L, M, Q, H }

enum class QrTab { Generate, Recognize, History }

class QrException(val code: String, message: String) : RuntimeException(message)

object QrEngine {
    const val SAMPLE_CONTENT = "https://github.com/rememberber/MooTool"
    const val DEFAULT_SIZE = 300
    const val MIN_SIZE = 120
    const val MAX_SIZE = 2000
    const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    const val MAX_IMAGE_EDGE = 4096
    private val dark = Color(0x11, 0x11, 0x11)
    private val light = Color.WHITE

    fun normalizeSize(size: Int): Int = size.coerceIn(MIN_SIZE, MAX_SIZE)

    fun generatePng(
        content: String,
        size: Int,
        correction: QrErrorCorrection,
        logo: BufferedImage? = null
    ): ByteArray {
        if (content.trim().isEmpty()) throw QrException("empty", "QR content is required")
        val width = normalizeSize(size)
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to levelOf(correction),
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 4
        )
        val matrix = try {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, width, hints)
        } catch (error: WriterException) {
            throw QrException("capacity", error.message ?: "Content is too large for a QR code")
        }
        val image = BufferedImage(width, width, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until width) {
            for (x in 0 until width) {
                image.setRGB(x, y, if (matrix.get(x, y)) dark.rgb else light.rgb)
            }
        }
        val withLogo = if (logo != null) overlayLogo(image, logo) else image
        return toPng(withLogo)
    }

    fun decode(image: BufferedImage): String {
        val prepared = flattenOnWhite(image)
        if (prepared.width > MAX_IMAGE_EDGE || prepared.height > MAX_IMAGE_EDGE) {
            throw QrException("too-large", "Image edge exceeds $MAX_IMAGE_EDGE pixels")
        }
        val pixels = IntArray(prepared.width * prepared.height)
        prepared.getRGB(0, 0, prepared.width, prepared.height, pixels, 0, prepared.width)
        val source = RGBLuminanceSource(prepared.width, prepared.height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = hashMapOf<DecodeHintType, Any>(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8",
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val reader = MultiFormatReader()
        val result = runCatching { reader.decode(bitmap, hints) }.getOrElse {
            reader.reset()
            hints[DecodeHintType.PURE_BARCODE] = true
            runCatching { reader.decode(bitmap, hints) }.getOrElse {
                throw QrException("not-found", "No QR code was found in the image")
            }
        }
        return result.text
    }

    fun decodePng(bytes: ByteArray): String = decode(readImage(bytes))

    fun decodeFile(path: Path): String {
        val size = Files.size(path)
        if (size > MAX_IMAGE_BYTES) throw QrException("too-large", "Image exceeds $MAX_IMAGE_BYTES bytes")
        val bytes = Files.readAllBytes(path)
        return decodePng(bytes)
    }

    fun readImage(bytes: ByteArray): BufferedImage {
        if (bytes.size > MAX_IMAGE_BYTES) throw QrException("too-large", "Image exceeds $MAX_IMAGE_BYTES bytes")
        return ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw QrException("invalid-image", "A valid image is required")
    }

    fun readImageFile(path: Path): BufferedImage {
        val size = Files.size(path)
        if (size > MAX_IMAGE_BYTES) throw QrException("too-large", "Image exceeds $MAX_IMAGE_BYTES bytes")
        return ImageIO.read(path.toFile())
            ?: throw QrException("invalid-image", "A valid image is required")
    }

    fun toPng(image: BufferedImage): ByteArray {
        val output = ByteArrayOutputStream()
        if (!ImageIO.write(image, "png", output)) {
            throw QrException("encode", "Unable to write PNG")
        }
        return output.toByteArray()
    }

    fun flattenOnWhite(image: BufferedImage): BufferedImage {
        val rgb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val graphics = rgb.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return rgb
    }

    private fun overlayLogo(qr: BufferedImage, logo: BufferedImage): BufferedImage {
        val size = qr.width
        val output = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val graphics = output.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(qr, 0, 0, size, size, null)
        val logoSize = (size * 0.2).toInt().coerceAtLeast(8)
        val x = (size - logoSize) / 2
        val y = (size - logoSize) / 2
        val padding = (size * 0.015).toInt().coerceAtLeast(4)
        graphics.color = Color.WHITE
        graphics.fillRect(x - padding, y - padding, logoSize + padding * 2, logoSize + padding * 2)
        graphics.drawImage(logo, x, y, logoSize, logoSize, null)
        graphics.dispose()
        return output
    }

    private fun levelOf(correction: QrErrorCorrection): ErrorCorrectionLevel = when (correction) {
        QrErrorCorrection.L -> ErrorCorrectionLevel.L
        QrErrorCorrection.M -> ErrorCorrectionLevel.M
        QrErrorCorrection.Q -> ErrorCorrectionLevel.Q
        QrErrorCorrection.H -> ErrorCorrectionLevel.H
    }
}
