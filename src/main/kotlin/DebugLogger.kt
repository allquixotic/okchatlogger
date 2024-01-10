/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.util.concurrent.atomic.AtomicReference
import javafx.application.Platform
import javafx.scene.control.TextArea
import org.slf4j.LoggerFactory

object DebugLogger {
    private val textAreaRef: AtomicReference<TextArea> = AtomicReference()
    private val logger = LoggerFactory.getLogger(DebugLogger::class.java)

    fun log(message: String) {
        val currentTextArea = textAreaRef.get()
        if (currentTextArea != null) {
            Platform.runLater {
                with(currentTextArea) {
                    appendText("$message\n")
                    selectPositionCaret(text.length)
                    deselect()
                }
            }
        } else {
            logger.warn(message)
        }
    }

    fun setLogger(textArea: TextArea) {
        this.textAreaRef.compareAndSet(null, textArea)
    }
}
