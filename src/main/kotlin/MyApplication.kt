/*
 * Copyright (c) 2024 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import javafx.application.Application
import javafx.stage.Stage

class MyApplication : Application() {

    override fun start(primaryStage: Stage) {
        ChatLoggerApp.initializeApp(primaryStage) // Initialize and show the ChatLoggerApp
    }

    companion object {
        fun fxLaunch() {
            launch(MyApplication::class.java)
        }
    }
}
