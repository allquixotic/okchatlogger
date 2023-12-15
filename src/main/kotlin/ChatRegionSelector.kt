/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.awt.GraphicsEnvironment
import java.awt.event.ActionEvent
import javax.swing.JFrame

class ChatRegionSelector(private val app: ChatLoggerApp) {

    fun selectChatRegion(ignored: ActionEvent) {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices
        for (screen in screens) {
            JFrame("Draw Rectangle").apply {
                app.chatRegionFrames.add(this)
                val drawingPanel = DrawingPanel(this, screen, app)
                add(drawingPanel)
                bounds = screen.defaultConfiguration.bounds
            }
        }
    }
}
