/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */

import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.function.Consumer
import org.apache.batik.transcoder.image.ImageTranscoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

object ChatLoggerApp {
    var screenComboBox = JComboBox<String>()
    val frame = JFrame("Chat Logger")
    private val intervalField = JSpinner(SpinnerNumberModel(Settings.interval, 30, 65536, 1)).apply {
        preferredSize = Dimension(60, 25)
        addChangeListener { Settings.interval = (value as Number).toInt() }
    }
    private val filePathField = JTextField(30).apply {
        documentListen(this) { text = Settings.logFilePath }
    }
    val xField = JSpinner(SpinnerNumberModel(Settings.x, 0, 65536, 1)).apply {
        addChangeListener { Settings.x = (value as Number).toInt() }
    }
    val yField = JSpinner(SpinnerNumberModel(Settings.y, 0, 65536, 1)).apply {
        addChangeListener { Settings.y = (value as Number).toInt() }
    }
    val widthField = JSpinner(SpinnerNumberModel(Settings.width, 1, 65536, 1)).apply {
        addChangeListener { Settings.width = (value as Number).toInt() }
    }
    val heightField = JSpinner(SpinnerNumberModel(Settings.height, 1, 65536, 1)).apply {
        addChangeListener { Settings.height = (value as Number).toInt() }
    }
    private val endpointField = JTextField(30).apply {
        documentListen(this) { text = Settings.endpoint }
    }
    private val apiKeyField = JPasswordField(30).apply {
        documentListen(this) { text = Settings.apiKey }
    }

    private val startButton = JButton("Start Logging")
    private val stopButton = JButton("Stop Logging").apply { isEnabled = false }
    private val selectRegionButton = JButton("Select Chat Window")
    private var timer: Timer? = null
    val chatRegionFrames = mutableListOf<JFrame>()
    internal var selectedScreen: GraphicsDevice? = null
    private val panel = JPanel()
    private val logTextArea = JTextArea(20, 60).apply {
        isEditable = false
        wrapStyleWord = true
        lineWrap = true
    }
    private val logScrollPane = JScrollPane(logTextArea).apply {
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    }
    private val popupWindowBox = JCheckBox("Show latest screenshot in popup?")
    private var popupFrame: JFrame? = null
    private var oldImage: BufferedImage? = null
    private var logFile: File? = null

    // Helper class instances
    private val ocr = AzureOcr()
    private val taker = ScreenshotTaker()
    private var chatLogger: ChatLogger? = null
    private var parser: TextParsing? = null
    private val regionSelector = ChatRegionSelector(this)

    private fun documentListen(jtc: JTextComponent, doit: Consumer<JTextComponent>) {
        doit.accept(jtc)
        jtc.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                doit.accept(jtc)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                doit.accept(jtc)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                // This method is not needed for plain text components
            }
        })
    }

    private fun getAppIcon(): BufferedImage {
        val svgData = this::class.java.classLoader.getResourceAsStream("kchatlogger.svg")?.readAllBytes()
        println("Read ${svgData!!.size} bytes of SVG.")
        val svgInputStream = ByteArrayInputStream(svgData)
        val inputSvgImage = TranscoderInput(svgInputStream)
        val imgTranscoder = object : ImageTranscoder() {
            lateinit var image: BufferedImage
            override fun createImage(w: Int, h: Int): BufferedImage {
                return BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            }

            override fun writeImage(img: BufferedImage, output: TranscoderOutput?) {
                this.image = img
            }
        }

        imgTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, 256f) // Set the width as needed
        imgTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, 256f) // Set the height as needed
        val outputStream = ByteArrayOutputStream()
        val outputSvgImage = TranscoderOutput(outputStream)
        imgTranscoder.transcode(inputSvgImage, outputSvgImage)
        return imgTranscoder.image
    }

    init {
        DebugLogger.setLogger(logTextArea)
        with(frame) {
            val gai = getAppIcon()
            iconImage = gai
            try {
                Taskbar.getTaskbar().iconImage = gai
            }
            catch(uoe: UnsupportedOperationException) {
                DebugLogger.log("No taskbar icon image support on this platform")
            }
            val intervalLabel = JLabel("Interval (sec):")
            val filePathLabel = JLabel("Log File Path:")
            val xLabel = JLabel("X:")
            val yLabel = JLabel("Y:")
            val widthLabel = JLabel("Width:")
            val heightLabel = JLabel("Height:")
            val endpointLabel = JLabel("Endpoint:")
            val apiKeyLabel = JLabel("API Key:")
            startButton.addActionListener { startLogging() }
            stopButton.addActionListener { stopLogging() }
            selectRegionButton.addActionListener { regionSelector.selectChatRegion(it) }
            val screenLabel = JLabel("Select Monitor")
            screenComboBox = JComboBox<String>().apply {
                val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                removeAllItems()
                screens.forEach { addItem(it.iDstring) }
                addItemListener { it: ItemEvent ->
                    if (it.stateChange == ItemEvent.SELECTED) {
                        val selectedID = it.item.toString()
                        selectedScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.find { it.iDstring == selectedID }
                    }
                }
            }
            panel.layout = GridBagLayout()
            val c = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; weightx = 1.0; insets = Insets(3, 3, 3, 3) }
            var row = 0
            arrayOf(
                arrayOf(endpointLabel, endpointField), arrayOf(apiKeyLabel, apiKeyField),
                arrayOf(intervalLabel, intervalField), arrayOf(filePathLabel, filePathField),
                arrayOf(screenLabel, screenComboBox), arrayOf(xLabel, xField), arrayOf(yLabel, yField),
                arrayOf(widthLabel, widthField), arrayOf(heightLabel, heightField), arrayOf(popupWindowBox),
                arrayOf(startButton, stopButton), arrayOf(selectRegionButton)
            ).forEach {
                var column = 0
                it.forEach { component ->
                    //addToPanel(component, c, column++, row, if (column == 0) GridBagConstraints.WEST else GridBagConstraints.EAST)
                    addToPanel(component, c, column++, row, GridBagConstraints.WEST)
                }
                row++
            }
            c.apply { gridwidth = 2 }
            addToPanel(logScrollPane, c, 0, row++, GridBagConstraints.SOUTH)
            add(panel)
            pack()
            isResizable = false
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        }
    }

    private fun addToPanel(comp: Component, c: GridBagConstraints, x: Int, y: Int, ancho: Int = GridBagConstraints.WEST) {
        with(c) {
            gridx = x
            gridy = y
            if (comp is JLabel) {
                weightx = 0.0
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.NONE
            } else if (comp is JSpinner || comp is JTextComponent || comp is JComboBox<*>) {
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = ancho
            } else if (comp is JButton || comp is JCheckBox) {
                // Buttons and checkboxes might need different configurations
                weightx = 0.0
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.NONE
            }
            insets = Insets(3, 3, 3, 3)
        }
        panel.add(comp, c)
    }



    private fun startLogging() {
        Settings.logFilePath = filePathField.text
        Settings.interval = (intervalField.value as Number).toInt()
        Settings.x = (xField.value as Number).toInt()
        Settings.y = (yField.value as Number).toInt()
        Settings.width = (widthField.value as Number).toInt()
        Settings.height = (heightField.value as Number).toInt()
        Settings.endpoint = endpointField.text
        Settings.apiKey = apiKeyField.password.toString()
        if (filePathField.text.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val interval: Int
        val x: Int
        val y: Int
        val width: Int
        val height: Int
        try {
            interval = (intervalField.value as Number).toInt() * 1000
            x = (xField.value as Number).toInt()
            y = (yField.value as Number).toInt()
            width = (widthField.value as Number).toInt()
            height = (heightField.value as Number).toInt()
        } catch (ignored: NumberFormatException) {
            JOptionPane.showMessageDialog(frame, "Please enter valid numbers for the fields.", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (interval <= 0 || width <= 0 || height <= 0 || x < 0 || y < 0) {
            JOptionPane.showMessageDialog(frame, "Interval, width, and height must be positive values. x and y must be non-negative.", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val fn = Settings.logFilePath
        if (fn.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "File name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (popupWindowBox.isSelected) {
            popupFrame = JFrame("Current Screenshot").apply {
                defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            }
        }

        logFile = File(fn)
        chatLogger = ChatLogger(logFile!!)
        parser = TextParsing(chatLogger!!)
        val chatWindowDimensions = Rectangle(x, y, width, height)
        val tickFn = ActionListener {
            DebugLogger.log("Interval Tick")
            val image = ScreenshotTaker.captureScreenshot(selectedScreen, chatWindowDimensions)
            val sim = ImageSimilarity.calculateMSE(image, oldImage)
            oldImage = image
            if (sim >= 0.0 && sim < 100.0) {
                DebugLogger.log("Image too similar to the last; not re-OCRing.")
                return@ActionListener
            }
            if (popupWindowBox.isSelected) {
                val imageLabel = JLabel(ImageIcon(image))
                if (popupFrame == null) {
                    popupFrame = JFrame("Current Screenshot").apply {
                        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    }
                }
                popupFrame!!.contentPane.removeAll()
                popupFrame!!.contentPane.add(imageLabel)
                popupFrame!!.pack()
                popupFrame!!.isVisible = true
            }
            val text = ocr.performOCR(image)
            parser!!.logChat(text)
        }
        timer = Timer(interval, tickFn)
        timer!!.start()
        startButton.isEnabled = false
        stopButton.isEnabled = true
        tickFn.actionPerformed(null)
    }

    private fun stopLogging() {
        timer?.stop()
        startButton.isEnabled = true
        stopButton.isEnabled = false
        popupFrame?.dispose()
        popupFrame = null
    }
}

