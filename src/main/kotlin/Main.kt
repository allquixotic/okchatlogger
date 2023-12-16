/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import com.formdev.flatlaf.FlatDarkLaf
import javax.swing.SwingUtilities
import javax.swing.UIManager
import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
		//GraalVM Fix
        if (System.getProperty("java.home") == null) {
            println("No Java Home set, assuming that we are running from GraalVM. Fixing...")
            System.setProperty("java.home", File(".").absolutePath)
        }
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