/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import kotlin.math.min
import kotlin.math.pow

object ImageSimilarity {

    private fun cropToMatch(img1: Image, img2: Image): Image {
        val width = min(img1.width.toInt(), img2.width.toInt())
        val height = min(img1.height.toInt(), img2.height.toInt())
        val reader: PixelReader = img1.pixelReader
        return WritableImage(reader, width, height)
    }

    /**
     * Calculate the Mean Squared Error (MSE) between two images.
     * @param img1 First image
     * @param img2 Second image
     * @return -1.0 if either parameter is null; 0.0 for an exact match; and a positive number if there's a difference.
     * For image sizes we usually use, 100.0 is a good threshold for "different" images.
     * Most images with completely different contents will return values in the thousands.
     */
    fun calculateMSE(img1: Image?, img2: Image?): Double {
        if (img1 == null || img2 == null) {
            return -1.0
        }

        var img1Temp = img1
        var img2Temp = img2

        // Crop the larger image to match the size of the smaller one
        if (img1Temp.width.toInt() != img2Temp.width.toInt() || img1Temp.height.toInt() != img2Temp.height.toInt()) {
            if (img1Temp.width * img1Temp.height < img2Temp.width * img2Temp.height) {
                img2Temp = cropToMatch(img2Temp, img1Temp)
            } else {
                img1Temp = cropToMatch(img1Temp, img2Temp)
            }
        }

        val pixelReader1: PixelReader = img1Temp.pixelReader
        val pixelReader2: PixelReader = img2Temp.pixelReader
        var mse = 0.0

        for (y in 0 until img1Temp.height.toInt()) {
            for (x in 0 until img1Temp.width.toInt()) {
                val color1 = pixelReader1.getColor(x, y)
                val color2 = pixelReader2.getColor(x, y)

                mse += (color1.red - color2.red).pow(2.0) + (color1.green - color2.green).pow(2.0) + (color1.blue - color2.blue).pow(2.0)
            }
        }

        mse /= (img1Temp.width * img1Temp.height * 3.0)
        return mse
    }
}
