/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.awt.GraphicsDevice
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.awt.GraphicsEnvironment

class ScreenshotTaker {
    companion object {
        fun takeScreenshot(gd: GraphicsDevice, rect: Rectangle): BufferedImage {
            val (translatedX, translatedY) = getTranslatedCoordinates(gd, rect.x, rect.y)
            val robot = Robot()
            return robot.createScreenCapture(Rectangle(translatedX, translatedY, rect.width, rect.height))
        }

        private fun getTranslatedCoordinates(gd: GraphicsDevice, x: Int, y: Int): Pair<Int, Int> {
            val bounds = gd.defaultConfiguration.bounds
            return Pair(x + bounds.x, y + bounds.y)
        }

        fun captureScreenshot(selectedScreen: GraphicsDevice?, chatWindowDimensions: Rectangle): BufferedImage {
            val gd = selectedScreen ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
            return takeScreenshot(gd, chatWindowDimensions)
        }
    }
}
