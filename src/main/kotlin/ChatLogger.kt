/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */

import java.io.File
import java.util.Date

class ChatLogger(private val logFile: File) {

    init {
        logChat("====Starting Logging on: ${Date().toString()}====")
    }

    fun logChat(text: String) {
        logFile.appendText("$text\n")
    }
}
