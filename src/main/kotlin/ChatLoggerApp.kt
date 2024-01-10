/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */

import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import kotlin.concurrent.timerTask

object ChatLoggerApp {
    private val screensMap by lazy {
        Screen.getScreens().mapIndexed { index, screen ->
            index to screen
        }.toMap()
    }

    lateinit var frame: Stage
    val chatRegionFrames: MutableList<Stage> = mutableListOf()
    var selectedScreen: Screen? = null
    val screenComboBox by lazy {
        ComboBox<String>().apply {
            items.addAll(screensMap.keys.map { "Screen ${it + 1}" })
            valueProperty().addListener { _, _, newValue ->
                val screenIndex = newValue.removePrefix("Screen ").toInt() - 1
                Settings.monitor = screenIndex
                selectedScreen = getScreenById(screenIndex)
            }
            value = "Screen ${Settings.monitor + 1}"
        }
    }
    private val intervalField by lazy {
        Spinner<Int>(30, 65536, Settings.interval, 1).apply {
            prefWidth = 60.0
            prefHeight = 25.0
            valueProperty().addListener { _, _, newValue ->
                Settings.interval = newValue.toInt()
            }
        }
    }
    private val filePathField by lazy {
        TextField(Settings.logFilePath).apply {
            prefColumnCount = 30
            textProperty().addListener { _, _, newValue ->
                Settings.logFilePath = newValue
            }
        }
    }
    val xField by lazy {
        Spinner<Int>(0, 65536, Settings.x, 1).apply {
            valueProperty().addListener { _, _, newValue ->
                Settings.x = newValue.toInt()
            }
        }
    }
    val yField by lazy {
        Spinner<Int>(0, 65536, Settings.y, 1).apply {
            valueProperty().addListener { _, _, newValue ->
                Settings.y = newValue.toInt()
            }
        }
    }
    val widthField by lazy {
        Spinner<Int>(1, 65536, Settings.width, 1).apply {
            valueProperty().addListener { _, _, newValue ->
                Settings.width = newValue.toInt()
            }
        }
    }
    val heightField by lazy {
        Spinner<Int>(1, 65536, Settings.height, 1).apply {
            valueProperty().addListener { _, _, newValue ->
                Settings.height = newValue.toInt()
            }
        }
    }
    private val endpointField by lazy {
        TextField(Settings.endpoint).apply {
            prefColumnCount = 30
            textProperty().addListener { _, _, newValue ->
                Settings.endpoint = newValue
            }
        }
    }
    private val apiKeyField by lazy {
        PasswordField().apply {
            prefColumnCount = 30
            textProperty().addListener { _, _, newValue ->
                Settings.apiKey = newValue
            }
            text = Settings.apiKey
        }
    }
    private val startButton by lazy {
        Button("Start Logging").apply {
            setOnAction { startLogging() }
        }
    }
    private val stopButton by lazy {
        Button("Stop Logging").apply {
            isDisable = true
            setOnAction { stopLogging() }
        }
    }
    private val ssButton by lazy {
        Button("Test Screenshot").apply {
            isDisable = false
            setOnAction { showDefaultPopup() }
        }
    }
    private val selectRegionButton by lazy {
        Button("Select Chat Window").apply {
            setOnAction { regionSelector.selectChatRegion() }
        }
    }
    private val logTextArea by lazy {
        TextArea().apply {
            isEditable = false
        }
    }
    private val popupWindowBox by lazy {
        CheckBox("Show latest screenshot in popup?")
    }
    private var popupStage: Stage? = null

    // Helper class instances
    private val ocr = AzureOcr()
    private var chatLogger: ChatLogger? = null
    private var parser: TextParsing? = null
    private val regionSelector by lazy {
        ChatRegionSelector(this)
    }
    private var timer: Timer? = null
    private var oldImage: Image? = null
    private var logFile: File? = null

    fun initializeApp(stage: Stage) {
        frame = stage
        frame.title = "Chat Logger App"
        val pngData = this::class.java.classLoader.getResourceAsStream("kchatlogger.png")?.readAllBytes()
        frame.icons.add(Image(ByteArrayInputStream(pngData)))
        val layout = VBox(10.0).apply {
            padding = Insets(10.0)
            children.addAll(
                HBox(10.0, Label("Interval (sec):"), intervalField),
                HBox(10.0, Label("Log File Path:"), filePathField, Button("Browse").apply {
                    setOnAction {
                        val fileChooser = FileChooser()
                        val selectedFile = fileChooser.showOpenDialog(frame)
                        if (selectedFile != null) {
                            filePathField.text = selectedFile.absolutePath
                        }
                    }
                }),
                HBox(10.0, Label("Select Monitor"), screenComboBox),
                HBox(10.0, Label("X:"), xField),
                HBox(10.0, Label("Y:"), yField),
                HBox(10.0, Label("Width:"), widthField),
                HBox(10.0, Label("Height:"), heightField),
                HBox(10.0, Label("Endpoint:"), endpointField),
                HBox(10.0, Label("API Key:"), apiKeyField),
                popupWindowBox,
                HBox(10.0, startButton, stopButton, ssButton),
                selectRegionButton,
                logTextArea
            )
        }

        DebugLogger.setLogger(logTextArea)
        // Set the application icon using the PNG image
        layout.styleClass.add(JMetroStyleClass.BACKGROUND)
        val scene = Scene(layout)
        JMetro(scene, Style.DARK)
        frame.scene = scene
        frame.setOnCloseRequest { System.exit(0) }
        frame.show()
    }

    fun getScreenById(id: Int): javafx.stage.Screen? {
        return screensMap[id]
    }

    fun getScreenIdByScreen(screen: Screen): Int? {
        return screensMap.entries.firstOrNull { it.value == screen }?.key
    }

    private fun startLogging() {
        Settings.logFilePath = filePathField.text
        Settings.interval = intervalField.value
        Settings.x = xField.value
        Settings.y = yField.value
        Settings.width = widthField.value
        Settings.height = heightField.value
        Settings.endpoint = endpointField.text
        Settings.apiKey = apiKeyField.text

        if (Settings.logFilePath.isEmpty()) {
            Alert(Alert.AlertType.ERROR, "All fields are required!").showAndWait()
            return
        }

        val intervalMillis = Settings.interval * 1000L
        val chatWindowDimensions = Rectangle2D(Settings.x.toDouble(), Settings.y.toDouble(),
            Settings.width.toDouble(), Settings.height.toDouble()
        )

        try {
            if (intervalMillis <= 0 || Settings.width <= 0 || Settings.height <= 0 || Settings.x < 0 || Settings.y < 0) {
                throw IllegalArgumentException("Interval, width, and height must be positive values. x and y must be non-negative.")
            }

            logFile = File(Settings.logFilePath)
            chatLogger = ChatLogger(logFile!!)
            parser = TextParsing(chatLogger!!)

            timer = Timer().apply {
                scheduleAtFixedRate(timerTask {
                    Platform.runLater {
                        DebugLogger.log("Interval Tick")
                        val image = ScreenshotTaker.captureScreenshot(getScreenById(Settings.monitor), chatWindowDimensions)
                        val sim = ImageSimilarity.calculateMSE(image, oldImage)
                        oldImage = image

                        if (sim >= 0.0 && sim < 100.0) {
                            DebugLogger.log("Image too similar to the last; not re-OCRing.")
                        } else {
                            showPopup(image)
                            val text = ocr.performOCR(image)
                            parser!!.logChat(text)
                        }
                    }
                }, 0, intervalMillis)
            }

            startButton.isDisable = true
            stopButton.isDisable = false

        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR, e.message).showAndWait()
        }
    }


    private fun stopLogging() {
        timer?.cancel()
        startButton.isDisable = false
        stopButton.isDisable = true
        popupStage?.close()
        popupStage = null
    }

    fun showPopup(image: Image, shouldShow: Boolean = popupWindowBox.isSelected) {
        if (shouldShow) {
            val imageLabel = ImageView(image)

            if (popupStage == null) {
                popupStage = Stage().apply {
                    title = "Current Screenshot"
                }
            }
            popupStage?.scene = Scene(VBox(imageLabel))
            popupStage?.sizeToScene()
            popupStage?.show()
        }
    }

    fun showDefaultPopup() {
        showPopup(ScreenshotTaker.captureDefaultScreenshot(), true)
    }
}

