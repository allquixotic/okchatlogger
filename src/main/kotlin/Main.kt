/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.io.File


class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            //This block is disabled because it doesn't work when native compiling with Gluon.
//            if (System.getProperty("os.name").lowercase().contains("mac")) {
//                val tk = Toolkit.getDefaultToolkit()
//                val image: java.awt.Image = tk.getImage(this::class.java.classLoader.getResource("kchatlogger.png"))
//                val taskbar = Taskbar.getTaskbar()
//                taskbar.iconImage = image
//            }

            //Garbage collect of AWT
            //System.gc()

            /*Thread.getAllStackTraces().keys.forEach {
                println(it.name)
            }*/

            // GraalVM Fix
            if (System.getProperty("java.home") == null) {
                println("No Java Home set, assuming that we are running from GraalVM. Fixing...")
                System.setProperty("java.home", File(".").absolutePath)
            }
            MyApplication.fxLaunch()
        }
    }
}
