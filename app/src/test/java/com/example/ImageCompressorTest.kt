package com.example

import android.graphics.Bitmap
import com.example.util.ImageCompressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageCompressorTest {

    @Test
    fun testFileSizeFormatting() {
        assertEquals("500 B", ImageCompressor.formatFileSize(500L))
        assertEquals("1.5 KB", ImageCompressor.formatFileSize(1536L))
        assertEquals("2.0 MB", ImageCompressor.formatFileSize(2 * 1024 * 1024L))
    }

    @Test
    fun testCompressFile() {
        val tempDir = File.createTempFile("test_compress", "").apply {
            delete()
            mkdirs()
        }
        val sourceFile = File(tempDir, "sample.jpg")
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        FileOutputStream(sourceFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val destFile = File(tempDir, "sample_compressed.jpg")
        val success = ImageCompressor.compressFile(sourceFile, destFile, maxDimension = 600, quality = 80)
        assertTrue(success)
        assertTrue(destFile.exists())
        assertTrue(destFile.length() > 0)

        // Thumbnail test
        val thumbFile = File(tempDir, "sample_thumb.jpg")
        val thumbSuccess = ImageCompressor.createThumbnail(destFile, thumbFile, size = 300, quality = 70)
        assertTrue(thumbSuccess)
        assertTrue(thumbFile.exists())

        tempDir.deleteRecursively()
    }
}
