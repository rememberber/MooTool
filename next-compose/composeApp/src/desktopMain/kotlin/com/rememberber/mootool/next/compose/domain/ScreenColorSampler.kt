package com.rememberber.mootool.next.compose.domain

import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

data class ScreenCapture(
    val originX: Int,
    val originY: Int,
    val image: BufferedImage
)

object ScreenColorSampler {
    fun captureAllScreens(): ScreenCapture {
        val devices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        if (devices.isEmpty()) throw ColorException("picker", "No screen is available")
        val union = Rectangle()
        devices.forEach { union.add(it.defaultConfiguration.bounds) }
        if (union.width <= 0 || union.height <= 0) throw ColorException("picker", "Screen bounds are empty")
        val mosaic = BufferedImage(union.width, union.height, BufferedImage.TYPE_INT_RGB)
        val graphics = mosaic.createGraphics()
        try {
            devices.forEach { device ->
                val bounds = device.defaultConfiguration.bounds
                val robot = Robot(device)
                val shot = robot.createScreenCapture(Rectangle(0, 0, bounds.width, bounds.height))
                graphics.drawImage(shot, bounds.x - union.x, bounds.y - union.y, null)
            }
        } catch (error: Exception) {
            throw ColorException("permission", error.message ?: "Screen capture is unavailable")
        } finally {
            graphics.dispose()
        }
        if (isUniformBlack(mosaic)) {
            throw ColorException("permission", "Screen capture returned a blank image; grant screen recording permission and retry")
        }
        return ScreenCapture(union.x, union.y, mosaic)
    }

    fun colorAt(capture: ScreenCapture, screenX: Int, screenY: Int): RgbColor {
        val x = (screenX - capture.originX).coerceIn(0, capture.image.width - 1)
        val y = (screenY - capture.originY).coerceIn(0, capture.image.height - 1)
        val rgb = capture.image.getRGB(x, y)
        return RgbColor((rgb shr 16) and 0xff, (rgb shr 8) and 0xff, rgb and 0xff)
    }

    fun zoom(capture: ScreenCapture, screenX: Int, screenY: Int, radius: Int = 5, scale: Int = 8): BufferedImage {
        val sourceX = (screenX - capture.originX).coerceIn(0, capture.image.width - 1)
        val sourceY = (screenY - capture.originY).coerceIn(0, capture.image.height - 1)
        val size = radius * 2 + 1
        val zoomed = BufferedImage(size * scale, size * scale, BufferedImage.TYPE_INT_RGB)
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val sx = (sourceX + dx).coerceIn(0, capture.image.width - 1)
                val sy = (sourceY + dy).coerceIn(0, capture.image.height - 1)
                val rgb = capture.image.getRGB(sx, sy)
                val px = (dx + radius) * scale
                val py = (dy + radius) * scale
                for (yy in 0 until scale) {
                    for (xx in 0 until scale) {
                        zoomed.setRGB(px + xx, py + yy, rgb)
                    }
                }
            }
        }
        return zoomed
    }

    private fun isUniformBlack(image: BufferedImage): Boolean {
        val stepX = maxOf(1, image.width / 32)
        val stepY = maxOf(1, image.height / 32)
        for (y in 0 until image.height step stepY) {
            for (x in 0 until image.width step stepX) {
                if (image.getRGB(x, y) and 0xffffff != 0) return false
            }
        }
        return true
    }
}
