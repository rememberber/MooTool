package com.rememberber.mootool.next.compose.domain

import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

object ImageOrientation {
    fun jpegOrientation(bytes: ByteArray): Int {
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return 1
        var offset = 2
        while (offset + 4 <= bytes.size) {
            if (bytes[offset] != 0xFF.toByte()) return 1
            val marker = bytes[offset + 1].toInt() and 0xff
            if (marker == 0xDA || marker == 0xD9) return 1
            val size = ((bytes[offset + 2].toInt() and 0xff) shl 8) or (bytes[offset + 3].toInt() and 0xff)
            if (size < 2 || offset + 2 + size > bytes.size) return 1
            if (marker == 0xE1) {
                val start = offset + 4
                val end = offset + 2 + size
                if (end - start >= 6 &&
                    bytes[start] == 'E'.code.toByte() &&
                    bytes[start + 1] == 'x'.code.toByte() &&
                    bytes[start + 2] == 'i'.code.toByte() &&
                    bytes[start + 3] == 'f'.code.toByte()
                ) {
                    return parseTiffOrientation(bytes, start + 6, end)
                }
            }
            offset += 2 + size
        }
        return 1
    }

    fun apply(image: BufferedImage, orientation: Int): BufferedImage {
        if (orientation !in 2..8) return image
        val swap = orientation in 5..8
        val width = if (swap) image.height else image.width
        val height = if (swap) image.width else image.height
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()
        try {
            val transform = when (orientation) {
                2 -> AffineTransform.getScaleInstance(-1.0, 1.0).apply { translate(-image.width.toDouble(), 0.0) }
                3 -> AffineTransform.getRotateInstance(Math.PI, image.width / 2.0, image.height / 2.0)
                4 -> AffineTransform.getScaleInstance(1.0, -1.0).apply { translate(0.0, -image.height.toDouble()) }
                5 -> AffineTransform().apply {
                    concatenate(AffineTransform.getRotateInstance(-Math.PI / 2, 0.0, 0.0))
                    concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
                    translate(-image.width.toDouble(), -image.height.toDouble())
                }
                6 -> AffineTransform.getRotateInstance(Math.PI / 2, 0.0, 0.0).apply {
                    translate(0.0, -image.height.toDouble())
                }
                7 -> AffineTransform().apply {
                    concatenate(AffineTransform.getRotateInstance(Math.PI / 2, 0.0, 0.0))
                    concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
                    translate(-image.width.toDouble(), 0.0)
                }
                8 -> AffineTransform.getRotateInstance(-Math.PI / 2, 0.0, 0.0).apply {
                    translate(-image.width.toDouble(), 0.0)
                }
                else -> AffineTransform()
            }
            graphics.transform = transform
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return output
    }

    private fun parseTiffOrientation(bytes: ByteArray, start: Int, end: Int): Int {
        if (start + 8 > end) return 1
        val little = bytes[start] == 'I'.code.toByte() && bytes[start + 1] == 'I'.code.toByte()
        val big = bytes[start] == 'M'.code.toByte() && bytes[start + 1] == 'M'.code.toByte()
        if (!little && !big) return 1
        fun u16(at: Int): Int {
            if (at + 2 > end) return 0
            return if (little) {
                (bytes[at].toInt() and 0xff) or ((bytes[at + 1].toInt() and 0xff) shl 8)
            } else {
                ((bytes[at].toInt() and 0xff) shl 8) or (bytes[at + 1].toInt() and 0xff)
            }
        }
        fun u32(at: Int): Int {
            if (at + 4 > end) return 0
            return if (little) {
                (bytes[at].toInt() and 0xff) or
                    ((bytes[at + 1].toInt() and 0xff) shl 8) or
                    ((bytes[at + 2].toInt() and 0xff) shl 16) or
                    ((bytes[at + 3].toInt() and 0xff) shl 24)
            } else {
                ((bytes[at].toInt() and 0xff) shl 24) or
                    ((bytes[at + 1].toInt() and 0xff) shl 16) or
                    ((bytes[at + 2].toInt() and 0xff) shl 8) or
                    (bytes[at].toInt() and 0xff)
            }
        }
        val ifd = start + u32(start + 4)
        if (ifd + 2 > end) return 1
        val count = u16(ifd)
        for (index in 0 until count) {
            val entry = ifd + 2 + index * 12
            if (entry + 12 > end) return 1
            if (u16(entry) == 0x0112) {
                val value = u16(entry + 8)
                return if (value in 1..8) value else 1
            }
        }
        return 1
    }
}
