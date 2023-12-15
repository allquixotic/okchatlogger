/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JTextArea
import javax.swing.SwingUtilities

object DebugLogger {
    private val jta: AtomicReference<JTextArea> = AtomicReference()
    private val logger = org.slf4j.LoggerFactory.getLogger(ChatLogger::class.java)

    fun log(message: String) {
        if(jta.get() != null) {
            SwingUtilities.invokeLater {
                with(jta.get()) {
                    append("$message\n")
                    caretPosition = document.length
                }
            }
        }
        else {
            logger.warn(message)
        }
    }

    fun setLogger(jta: JTextArea) {
        this.jta.compareAndSet(null, jta)
    }
}