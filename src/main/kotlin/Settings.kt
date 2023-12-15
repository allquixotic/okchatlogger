/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import kotlin.properties.Delegates
import com.google.gson.Gson
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
import kotlin.math.roundToInt

data object Settings {
    @Transient
    private val saveScheduled = AtomicBoolean(false)
    var endpoint: String by Delegates.observable("https://your_domain.cognitiveservices.azure.com") { _, _, _ -> scheduleSave() }
    var apiKey: String by Delegates.observable("abcd1234") { _, _, _ -> scheduleSave() }
    var interval: Int by Delegates.observable(30) { _, _, _ -> scheduleSave() }
    var logFilePath: String by Delegates.observable(Path.of(dev.dirs.UserDirectories.get().documentDir, "chatlog.txt").toString()) { _, _, _ -> scheduleSave() }
    var monitor: String by Delegates.observable("") { _, _, _ -> scheduleSave() }
    var x: Int by Delegates.observable(0) { _, _, _ -> scheduleSave() }
    var y: Int by Delegates.observable(0) { _, _, _ -> scheduleSave() }
    var width: Int by Delegates.observable(400) { _, _, _ -> scheduleSave() }
    var height: Int by Delegates.observable(400) { _, _, _ -> scheduleSave() }

    private val configPath: String by lazy {
        val t = Path.of(dev.dirs.ProjectDirectories.from("org", "sokangaming", "chatlogger").configDir, "settings.json").toString()
        DebugLogger.log("Path to config: $t")
        t
    }

    init {
        load()
    }

    private fun load() {
        val file = File(configPath)
        if (file.exists()) {
            DebugLogger.log("Reading settings from $configPath")
            val loadedSettings = Gson().fromJson(file.readText(), Map::class.java)
            this.endpoint = loadedSettings["endpoint"] as String
            this.apiKey = loadedSettings["apiKey"] as String
            this.interval = loadedSettings["interval"].toString().toDouble().roundToInt()
            this.logFilePath = loadedSettings["logFilePath"] as String
            this.monitor = loadedSettings["monitor"] as String
            this.x = loadedSettings["x"].toString().toDouble().roundToInt()
            this.y = loadedSettings["y"].toString().toDouble().roundToInt()
            this.width = loadedSettings["width"].toString().toDouble().roundToInt()
            this.height = loadedSettings["height"].toString().toDouble().roundToInt()
        }
        else {
            DebugLogger.log("Loading default settings")
        }
    }

    private fun scheduleSave() {
        if (saveScheduled.compareAndSet(false, true)) {
            timer("SaveTimer", false, 1000, 1000) {
                save()
                this.cancel()
                saveScheduled.set(false)
            }
        }
    }

    private fun save() {
        DebugLogger.log("Saving settings to $configPath")
        val file = File(configPath)
        file.parentFile.mkdirs() // Ensure the directory structure exists
        file.writeText(Gson().toJson(hashMapOf(
            "endpoint" to endpoint,
            "apiKey" to apiKey,
            "interval" to interval,
            "logFilePath" to logFilePath,
            "monitor" to monitor,
            "x" to x,
            "y" to y,
            "width" to width,
            "height" to height
        )))
    }
}
