package com.hermex.android.looksmax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * High-performance, memory-safe image compressor tailored for Android 14+ / 17 (QPR0)
 * and high-resolution camera sensors (e.g. 108 MP Redmi Note 13).
 *
 * Prevents OutOfMemoryError and SecurityException by:
 * 1. Safely caching the input stream to a local temp file immediately.
 * 2. Decoding image bounds without allocating memory.
 * 3. Pre-calculating inSampleSize to never exceed 1600px in memory.
 * 4. Using RGB_565 (2 bytes/pixel) and recycling all intermediate bitmaps.
 */
object ImageCompressor {
    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 82

    fun compress(context: Context, source: Any): ByteArray? {
        var tempFile: File? = null
        return try {
            // Step 0: Ensure we have a local accessible File to avoid ContentResolver / SecurityException races
            val fileToRead: File = when (source) {
                is File -> source
                is Uri -> {
                    tempFile = File.createTempFile("compress_src_", ".tmp", context.cacheDir)
                    context.contentResolver.openInputStream(source)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: run {
                        Log.e(TAG, "Cannot open stream for URI: $source")
                        return null
                    }
                    tempFile
                }
                else -> return null
            }

            // Step 1: Decode dimensions only without loading pixel data
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            FileInputStream(fileToRead).use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                Log.e(TAG, "Invalid image bounds: ${origWidth}x${origHeight}")
                return null
            }

            // Step 2: Compute power-of-two sample size targeting MAX_DIMENSION
            var sampleSize = 1
            var w = origWidth
            var h = origHeight
            while (w / 2 >= MAX_DIMENSION || h / 2 >= MAX_DIMENSION) {
                w /= 2
                h /= 2
                sampleSize *= 2
            }

            // Step 3: Decode subsampled bitmap using RGB_565 (2 bytes/pixel instead of 4)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = true
            }

            val subsampledBitmap = FileInputStream(fileToRead).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: run {
                Log.e(TAG, "Failed to decode subsampled bitmap")
                return null
            }

            // Step 4: Handle EXIF orientation
            val orientation = runCatching {
                FileInputStream(fileToRead).use { stream ->
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val orientedBitmap = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(
                    subsampledBitmap,
                    0,
                    0,
                    subsampledBitmap.width,
                    subsampledBitmap.height,
                    matrix,
                    true
                )
                if (rotated != subsampledBitmap) {
                    subsampledBitmap.recycle()
                }
                rotated
            } else {
                subsampledBitmap
            }

            // Step 5: Downscale if still above MAX_DIMENSION
            val finalBitmap = if (orientedBitmap.width > MAX_DIMENSION || orientedBitmap.height > MAX_DIMENSION) {
                val ratio = MAX_DIMENSION.toFloat() / maxOf(orientedBitmap.width, orientedBitmap.height)
                val targetW = (orientedBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (orientedBitmap.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true)
                if (scaled != orientedBitmap) {
                    orientedBitmap.recycle()
                }
                scaled
            } else {
                orientedBitmap
            }

            // Step 6: Compress to JPEG
            val finalW = finalBitmap.width
            val finalH = finalBitmap.height
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            finalBitmap.recycle()

            val bytes = outputStream.toByteArray()
            Log.i(TAG, "Successfully compressed image: ${origWidth}x${origHeight} -> ${finalW}x${finalH} (${bytes.size} bytes)")
            bytes
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "Caught OutOfMemoryError while compressing photo", oom)
            System.gc()
            null
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error in ImageCompressor", t)
            null
        } finally {
            tempFile?.delete()
        }
    }
}
