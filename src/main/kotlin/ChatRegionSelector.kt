/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import Settings.x
import Settings.y
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.scene.Scene
import javafx.application.Platform
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.StageStyle
import kotlin.math.abs

class ChatRegionSelector(private val app: ChatLoggerApp) {

    fun selectChatRegion() {
        Platform.runLater {
            val screens = Screen.getScreens()
            for (screen in screens) {
                val stage = Stage()
                val screenId = ChatLoggerApp.getScreenIdByScreen(screen)!!
                var startPoint: javafx.geometry.Point2D? = null
                var endPoint: javafx.geometry.Point2D? = null
                val rect = Rectangle(0.0, 0.0, 1.0, 1.0)
                val screenDevice = ChatLoggerApp.getScreenById(screenId)!!
                val label = Text().apply {
                    text = "This is screen ${screenId+1}. Click and drag a rectangle to select a region. Press ESC to cancel."
                    font = Font.font("Serif", 40.0)
                    fill = Color.RED
                    //StackPane.setAlignment(this, javafx.geometry.Pos.CENTER)
                }
                val rootPane = Pane(rect, label)

                stage.isAlwaysOnTop = true
                app.chatRegionFrames.add(stage)
                rect.isVisible = false
                rect.fill = Color.rgb(0, 0, 255, 0.5)

                stage.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
                    startPoint = javafx.geometry.Point2D(event.x, event.y)
                    endPoint = javafx.geometry.Point2D(event.x+1, event.y+1)
                    rect.x = event.x
                    rect.y = event.y
                    rect.width = 1.0
                    rect.height = 1.0
                    rect.isVisible = true
                    rect.relocate(event.x, event.y)
                }

                stage.addEventHandler(MouseEvent.MOUSE_DRAGGED) { event ->
                    endPoint = javafx.geometry.Point2D(event.x, event.y)
                    val calcX = minOf(startPoint!!.x, endPoint!!.x)
                    val calcY = minOf(startPoint!!.y, endPoint!!.y)
                    val calcW = abs(startPoint!!.x - endPoint!!.x)
                    val calcH = abs(startPoint!!.y - endPoint!!.y)
                    with(rect) {
                        x = calcX
                        y = calcY
                        width = calcW
                        height = calcH
                    }
                }

                stage.addEventHandler(MouseEvent.MOUSE_RELEASED) { event ->
                    val calcX = minOf(startPoint!!.x, endPoint!!.x).toInt()
                    val calcY = minOf(startPoint!!.y, endPoint!!.y).toInt()
                    val calcW = abs(startPoint!!.x - endPoint!!.x).toInt()
                    val calcH = abs(startPoint!!.y - endPoint!!.y).toInt()
                    ChatLoggerApp.selectedScreen = screenDevice
                    ChatLoggerApp.screenComboBox.value = "Screen ${(ChatLoggerApp.getScreenIdByScreen(screenDevice) ?: 1) + 1}"
                    ChatLoggerApp.xField.valueFactory.value = calcX
                    ChatLoggerApp.yField.valueFactory.value = calcY
                    ChatLoggerApp.widthField.valueFactory.value = calcW
                    ChatLoggerApp.heightField.valueFactory.value = calcH
                    ChatLoggerApp.chatRegionFrames.forEach { it.close() }
                    ChatLoggerApp.chatRegionFrames.clear()
                }

                stage.addEventHandler(KeyEvent.KEY_RELEASED) { event ->
                    if (event.code == javafx.scene.input.KeyCode.ESCAPE) {
                        ChatLoggerApp.chatRegionFrames.forEach { it.close() }
                        ChatLoggerApp.chatRegionFrames.clear()
                    }
                }

                val scene = Scene(rootPane, screen.bounds.width, screen.bounds.height)
                stage.scene = scene
                stage.opacity = 0.25
                stage.initStyle(StageStyle.TRANSPARENT)
                label.layoutX = (scene.width - label.boundsInLocal.width) / 2
                label.layoutY = (scene.height - label.boundsInLocal.height) / 2
                // Listener to re-center the label if the scene size changes
                scene.widthProperty().addListener { _, _, _ ->
                    label.layoutX = (scene.width - label.boundsInLocal.width) / 2
                }
                scene.heightProperty().addListener { _, _, _ ->
                    label.layoutY = (scene.height - label.boundsInLocal.height) / 2
                }

                // Set stage properties
                val bounds = screen.bounds
                stage.x = bounds.minX
                stage.y = bounds.minY
                stage.width = bounds.width
                stage.height = bounds.height

                stage.show()
            }
        }
    }
}

