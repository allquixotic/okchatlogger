/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.GraphicsDevice
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class DrawingPanel(
    private val parentFrame: JFrame,
    private val screenDevice: GraphicsDevice,
    private val app: ChatLoggerApp
) : JPanel() {

    private var startPoint: Point? = null
    private var endPoint: Point? = null
    private var currentRectangle: Rectangle? = null
    private val label: JLabel

    init {
        layout = GridBagLayout()
        with(parentFrame) {
            isUndecorated = true
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            isAlwaysOnTop = true
            isVisible = true
            opacity = 0.85f
        }
        background = Color(255, 255, 255, 127) // Translucent white background

        label = JLabel().apply {
            text = "<html>This is ${screenDevice.iDstring}.<br>Click and drag a rectangle to select a region.<br>Press ESC to cancel.</html>"
            font = Font("Serif", Font.BOLD, 40)
            foreground = Color.RED
            isOpaque = true
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
        }

        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            anchor = GridBagConstraints.CENTER
        }
        add(label, constraints)

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                startPoint = e.point
                endPoint = null
                currentRectangle = null
                val col = Color(255, 255, 255, 0)
                SwingUtilities.invokeLater {
                    background = col
                    (app.chatRegionFrames.toList()).forEach { jf ->
                        if (jf != parentFrame) {
                            jf.dispose()
                            app.chatRegionFrames.remove(jf)
                        }
                    }
                }
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                currentRectangle?.let {
                    app.selectedScreen = screenDevice
                    app.screenComboBox.selectedItem = screenDevice.iDstring
                    app.xField.value = it.x
                    app.yField.value = it.y
                    app.widthField.value = it.width
                    app.heightField.value = it.height

                    app.chatRegionFrames.forEach { frame -> frame.dispose() }
                    app.chatRegionFrames.clear()
                }
            }
        })

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                endPoint = e.point
                val x = minOf(startPoint!!.x, endPoint!!.x)
                val y = minOf(startPoint!!.y, endPoint!!.y)
                val width = kotlin.math.abs(startPoint!!.x - endPoint!!.x)
                val height = kotlin.math.abs(startPoint!!.y - endPoint!!.y)
                currentRectangle = Rectangle(x, y, width, height)
                repaint()
            }
        })

        addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    app.chatRegionFrames.forEach { it.dispose() }
                    app.chatRegionFrames.clear()
                }
            }
        })

        isFocusable = true
        requestFocusInWindow()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = Color(0, 0, 255, 127) // Translucent blue color
        currentRectangle?.let {
            g.fillRect(it.x, it.y, it.width, it.height)
        }
    }
}

