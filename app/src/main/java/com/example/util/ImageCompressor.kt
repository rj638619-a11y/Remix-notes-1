package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageCompressor {

    private const val TAG = "ImageCompressor"
    const val DEFAULT_MAX_DIMENSION = 1920
    const val DEFAULT_QUALITY = 82
    const val THUMBNAIL_SIZE = 360
    const val THUMBNAIL_QUALITY = 75

    /**
     * Compresses an image from a content Uri to a destination File.
     * Downscales images larger than [maxDimension] and compresses using JPEG with [quality].
     * Handles EXIF orientation to keep photos correctly oriented.
     * Recycles bitmaps immediately to prevent memory leaks or OOM on low-end devices.
     */
    fun compressUri(
        context: Context,
        uri: Uri,
        outputFile: File,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_QUALITY
    ): Boolean {
        return try {
            // 1. Read EXIF orientation first while stream is fresh
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not read EXIF orientation: ${e.message}")
            }

            // 2. Decode image bounds only (0 MB heap allocation)
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return false

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return false
            }

            // 3. Compute optimal sample size (power of 2)
            val sampleSize = calculateInSampleSize(origWidth, origHeight, maxDimension, maxDimension)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            // 4. Decode downsampled bitmap
            val downsampledBitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            // 5. Scale smoothly to exact max dimension if needed and apply EXIF rotation
            val finalBitmap = transformBitmap(downsampledBitmap, maxDimension, orientation)

            // 6. Write compressed output to file
            outputFile.parentFile?.mkdirs()
            val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
            FileOutputStream(tempFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }

            if (finalBitmap != downsampledBitmap && !downsampledBitmap.isRecycled) {
                downsampledBitmap.recycle()
            }
            if (!finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (outputFile.exists()) outputFile.delete()
                tempFile.renameTo(outputFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image from URI", e)
            false
        }
    }

    /**
     * Compresses an existing image File to a destination File.
     */
    fun compressFile(
        sourceFile: File,
        outputFile: File,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_QUALITY
    ): Boolean {
        if (!sourceFile.exists()) return false
        return try {
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                val exif = ExifInterface(sourceFile.absolutePath)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Exception) {}

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return false

            val sampleSize = calculateInSampleSize(origWidth, origHeight, maxDimension, maxDimension)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val downsampled = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return false
            val finalBitmap = transformBitmap(downsampled, maxDimension, orientation)

            outputFile.parentFile?.mkdirs()
            val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
            FileOutputStream(tempFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }

            if (finalBitmap != downsampled && !downsampled.isRecycled) {
                downsampled.recycle()
            }
            if (!finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (outputFile.exists()) outputFile.delete()
                tempFile.renameTo(outputFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing file", e)
            false
        }
    }

    /**
     * Generates a lightweight square or proportionate thumbnail for gallery grids.
     */
    fun createThumbnail(
        sourceFile: File,
        thumbFile: File,
        size: Int = THUMBNAIL_SIZE,
        quality: Int = THUMBNAIL_QUALITY
    ): Boolean {
        return compressFile(sourceFile, thumbFile, maxDimension = size, quality = quality)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun transformBitmap(
        source: Bitmap,
        maxDimension: Int,
        orientation: Int
    ): Bitmap {
        val width = source.width
        val height = source.height

        val matrix = Matrix()

        // Handle rotation from EXIF
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        // Scale down if larger than maxDimension
        val maxCurrent = max(width, height)
        if (maxCurrent > maxDimension) {
            val scale = maxDimension.toFloat() / maxCurrent.toFloat()
            matrix.postScale(scale, scale)
        }

        return if (matrix.isIdentity) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, width, height, matrix, true)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
            else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        }
    }
}
