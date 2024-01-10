/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import ChatLoggerApp.getScreenById
import javafx.scene.image.WritableImage
import javafx.scene.robot.Robot
import javafx.geometry.Rectangle2D
import javafx.stage.Screen

class ScreenshotTaker {
    companion object {
        private fun takeScreenshot(screen: Screen, rect: Rectangle2D): WritableImage {
            val javafxRobot = Robot()
            return javafxRobot.getScreenCapture(null, rect)
        }

        private fun getTranslatedCoordinates(screen: Screen, x: Double, y: Double): Pair<Double, Double> {
            val bounds = screen.bounds
            return Pair(x + bounds.minX, y + bounds.minY)
        }

        fun captureScreenshot(selectedScreen: Screen?, chatWindowDimensions: Rectangle2D): WritableImage {
            val screen = selectedScreen ?: Screen.getPrimary()
            val (translatedX, translatedY) = getTranslatedCoordinates(screen, chatWindowDimensions.minX, chatWindowDimensions.minY)
            return takeScreenshot(screen, Rectangle2D(translatedX, translatedY, chatWindowDimensions.width, chatWindowDimensions.height))
        }

        fun captureDefaultScreenshot(): WritableImage {
            val chatWindowDimensions = Rectangle2D(Settings.x.toDouble(), Settings.y.toDouble(),
                Settings.width.toDouble(), Settings.height.toDouble()
            )
            val screen = getScreenById(Settings.monitor)
            return captureScreenshot(screen, chatWindowDimensions)
        }
    }
}
