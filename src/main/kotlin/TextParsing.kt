/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import org.apache.commons.text.similarity.JaroWinklerDistance

class TextParsing(private val chatLogger: ChatLogger) {
    private val prevLines = mutableListOf<String>()
    private var screenshotNum = 0

    fun parseChat(currentText: String): List<String> {
        val jwd = JaroWinklerDistance()
        return currentText.replace(Regex("\n\n"), "\n")
            .replace(Regex("\n"), "\n").split("\n")
            .filter { it.isNotBlank() && it.trim().replace(Regex("\\W"), "").isNotEmpty() }
            .filter { line ->
                val swt = line.replaceFirst(Regex("""^\s*[\[{(]+\d{1,2}:\d{2}:\d{2}\s*(AM|PM|am|pm)[]})]+\s*"""), "")
                    .replaceFirst(Regex("\\.$"), "")
                prevLines.all { pt -> jwd.apply(line, pt) > 0.05 } &&
                        jwd.apply(swt, "You are now away from keyboard") > 0.1 &&
                        jwd.apply(swt, "You are no longer away from keyboard") > 0.1 &&
                        jwd.apply(swt, "has completed a conquest objective") > 0.1
            }
    }

    fun logChat(currentText: String) {
        val currentLines = parseChat(currentText)
        val linesStr = currentLines.joinToString("\n").trim() + "\n"
        if (linesStr.isNotBlank()) {
            DebugLogger.log("=====\nScreenshot #${screenshotNum++}: Unique new text to log:\n$linesStr=====")
            chatLogger.logChat(linesStr)
            prevLines.addAll(currentLines)
        }
    }
}