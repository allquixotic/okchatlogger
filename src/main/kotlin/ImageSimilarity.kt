/*
 * Copyright (c) 2023 Sean McNamara <smcnam@gmail.com>. Distributed under the terms of Apache License 2.0.
 * See LICENSE.txt for details.
 */
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.pow

object ImageSimilarity {

    fun cropToMatch(img1: BufferedImage, img2: BufferedImage): BufferedImage {
        val width = min(img1.width, img2.width)
        val height = min(img1.height, img2.height)
        return img1.getSubimage(0, 0, width, height)
    }

    /**
     * Calculate the Mean Squared Error (MSE) between two images.
     * @param img1 First image
     * @param img2 Second image
     * @return -1.0 if either parameter is null; 0.0 for an exact match; and a positive number if there's a difference.
     * For image sizes we usually use, 100.0 is a good threshold for "different" images.
     * Most images with completely different contents will return values in the thousands.
     */
    fun calculateMSE(img1: BufferedImage?, img2: BufferedImage?): Double {
        if (img1 == null || img2 == null) {
            return -1.0
        }
        var img1Temp = img1
        var img2Temp = img2

        // Crop the larger image to match the size of the smaller one
        if (img1Temp.width != img2Temp.width || img1Temp.height != img2Temp.height) {
            if (img1Temp.width * img1Temp.height < img2Temp.width * img2Temp.height) {
                img2Temp = cropToMatch(img2Temp, img1Temp)
            } else {
                img1Temp = cropToMatch(img1Temp, img2Temp)
            }
        }

        var mse = 0.0
        for (y in 0 until img1Temp.height) {
            for (x in 0 until img1Temp.width) {
                val pixel1 = img1Temp.getRGB(x, y)
                val pixel2 = img2Temp.getRGB(x, y)
                val red1 = (pixel1 shr 16) and 0xff
                val green1 = (pixel1 shr 8) and 0xff
                val blue1 = pixel1 and 0xff
                val red2 = (pixel2 shr 16) and 0xff
                val green2 = (pixel2 shr 8) and 0xff
                val blue2 = pixel2 and 0xff
                mse += (red1 - red2).toDouble().pow(2.0) + (green1 - green2).toDouble().pow(2.0) + (blue1 - blue2).toDouble().pow(2.0)
            }
        }
        mse /= (img1Temp.width * img1Temp.height * 3.0)
        return mse
    }
}
