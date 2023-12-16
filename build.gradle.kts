import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include
import java.io.BufferedReader
import java.io.InputStream
import java.io.PrintStream
import java.time.Duration
import java.io.InputStreamReader

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("application")
    id("org.graalvm.buildtools.native") version "0.9.28"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.sokangaming"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("Main")
    if (project.hasProperty("mainclass")) {
        mainClass.set(project.property("mainclass") as String)
    }
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Main"
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("dev.dirs:directories:26")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17")
    implementation("org.apache.commons:commons-exec:1.3")
}


graalvmNative {
    agent {
        defaultMode.set("standard")
    }
    metadataRepository {
        enabled = true
    }
    binaries {
        all {
            buildArgs.addAll(
                listOf(
                    "--no-fallback",
                    "--enable-https",
                    "-Djava.awt.headless=false"
                )
            )
            resources.autodetect()
        }
    }
}

val operatingSystem = when {
    System.getProperty("os.name").lowercase().startsWith("mac", ignoreCase = true) -> "macosx"
    System.getProperty("os.name").lowercase().startsWith("windows", ignoreCase = true) -> "windows"
    else -> "linux"
}

fun executeCommandAndHandleOutput(command: Array<String>): Int {
    val gradleCommand = if (operatingSystem == "windows") ".\\gradlew.bat" else "./gradlew"
    val process = Runtime.getRuntime().exec(arrayOf(gradleCommand, *command))

    // Function to handle stream output
    fun handleStream(inputStream: InputStream, output: PrintStream) {
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.println(line)
            }
        }
    }

    // Creating and starting threads for handling stdout and stderr
    val stdoutThread = Thread { handleStream(process.inputStream, System.out) }
    val stderrThread = Thread { handleStream(process.errorStream, System.err) }

    stdoutThread.start()
    stderrThread.start()

    // Waiting for threads to finish
    stdoutThread.join()
    stderrThread.join()

    // Waiting for the process to complete and returning the exit value
    return process.waitFor()
}

tasks.register<Copy>("setupNativeImage") {
    // Define the destination directory
    val destDir = layout.projectDirectory.dir("src/main/resources/META-INF/native-image")

    // Set the destination directory for the copy task
    into(destDir)

    // Define the source directory
    val srcDir = layout.projectDirectory.dir("native-image-metadata/$operatingSystem")
    from(srcDir)
    include("**/*") // Include all files and directories recursively
}

tasks.named("processResources") {
    dependsOn("setupNativeImage")
}

tasks.named("nativeCompile") {
    dependsOn("setupNativeImage")
	
}

tasks.register("instrument") {
    doLast {
        var retval = executeCommandAndHandleOutput(arrayOf("-Pagent", "run"))
        if (retval == 0) {
            retval = executeCommandAndHandleOutput(arrayOf("metadataCopy", "--task", "run", "--dir", "src/main/resources/META-INF/native-image"))
            if (retval == 0) {
                retval = executeCommandAndHandleOutput(arrayOf("nativeCompile"))
            }
        }
    }
}

tasks.register("nativeDist") {
	dependsOn("nativeCompile") 
	doLast {
		// Copy .exe and .dll files
		copy {
			mkdir("build/dist")
			from("build/native/nativeCompile")
			into("build/dist")
			include("*.exe")
			include("*.dll")
		}

		// Print java.home property
		println(System.getProperty("java.home"))

		// Copy specific files from java.home/lib to build/dist/lib
		copy {
			val javaHome = System.getProperty("java.home") ?: throw GradleException("java.home property not found")
			
			mkdir("build/dist/lib")
			from("$javaHome/lib")
			into("build/dist/lib")
			include("fontconfig.bfc")
			include("fontconfig.properties.src")
			include("psfont.properties.ja")
			include("psfontj2d.properties")
		}
	}
}
