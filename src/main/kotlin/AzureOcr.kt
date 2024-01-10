/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */

import com.google.gson.Gson
import javafx.scene.image.Image
import org.glavo.png.PNGType
import org.glavo.png.javafx.PNGJavaFXUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AzureOcr(private val endpoint: String = Settings.endpoint,
               private val apiKey: String = Settings.apiKey) {

    private val gson = Gson()

    fun performOCR(image: Image): String {
        // Convert BufferedImage to PNG
        val imageBytes = PNGJavaFXUtils.writeImageToArray(image, PNGType.RGBA, 9)

        // Prepare the request
        val slash = if (endpoint.endsWith("/")) "" else "/"
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${endpoint}${slash}computervision/imageanalysis:analyze?api-version=2023-04-01-preview&features=read&language=en"))
            .header("Content-Type", "image/png")
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofByteArray(imageBytes))
            .build()

        // Send the request and get the response
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val responseText = extractTextFromResponse(response.body())
        return responseText
    }

    private fun extractTextFromResponse(responseBody: String): String {
        val resp = gson.fromJson(responseBody, Map::class.java) as Map<*, *>
        if (resp.containsKey("readResult")) {
            val readResult = resp["readResult"] as Map<*, *>
            val extractedText = readResult["content"] as String
            DebugLogger.log("Azure returned the following:\n========\n${extractedText}\n========")
            return extractedText
        }
        else {
            DebugLogger.log("Azure didn't return a readResult!")
            return ""
        }
    }
}
