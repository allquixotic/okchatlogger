/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import com.formdev.flatlaf.FlatDarkLaf
import java.awt.Taskbar
import javax.swing.SwingUtilities
import javax.swing.UIManager

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            val osName = System.getProperty("os.name").replace(" ", "").lowercase()
            val isMac = osName == "macosx"
            if (isMac) {
                System.setProperty("apple.awt.application.appearance", "system")
            }
            UIManager.setLookAndFeel(FlatDarkLaf())
            ChatLoggerApp.frame.isVisible = true
        }
    }
}