package com.example.vectorscan

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object ImageProcessor {
    private const val TAG = "ImageProcessor"

    fun initOpenCV(): Boolean {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV loaded successfully")
            return true
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            return false
        }
    }

    /**
     * Convert bitmap to grayscale for basic preview
     */
    fun bitmapToGrayscale(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
        
        val resultBitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(grayMat, resultBitmap)
        
        // Clean up
        mat.release()
        grayMat.release()
        
        return resultBitmap
    }

    /**
     * Process image to detect edges and prepare for vectorization
     * @param bitmap Input image
     * @param thresholdValue Threshold for edge detection (0-255), lower = more detail
     * @return Processed binary image
     */
    fun preprocessForVectorization(bitmap: Bitmap, thresholdValue: Int = 128): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
        
        // Apply Gaussian blur to reduce noise
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0)
        
        // Apply adaptive threshold or simple threshold
        val thresholdMat = Mat()
        Imgproc.threshold(blurredMat, thresholdMat, thresholdValue.toDouble(), 255.0, Imgproc.THRESH_BINARY)
        
        val resultBitmap = Bitmap.createBitmap(thresholdMat.cols(), thresholdMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(thresholdMat, resultBitmap)
        
        // Clean up
        mat.release()
        grayMat.release()
        blurredMat.release()
        thresholdMat.release()
        
        return resultBitmap
    }

    /**
     * Extract contours from processed image
     * @param bitmap Preprocessed binary image
     * @param epsilon Approximation accuracy (higher = simpler polygons)
     * @return List of contours, each contour is a list of points
     */
    fun extractContours(bitmap: Bitmap, epsilon: Double = 2.0): List<List<Point>> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale if needed
        val grayMat = if (mat.channels() > 1) {
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            gray
        } else {
            mat.clone()
        }
        
        // Find contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(grayMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        Log.d(TAG, "Found ${contours.size} contours")
        
        // Approximate contours to polygons and convert to simple points
        val simplifiedContours = mutableListOf<List<Point>>()
        
        for (contour in contours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val approx = MatOfPoint2f()
            val arcLength = Imgproc.arcLength(contour2f, true)
            
            // Approximate to polygon
            Imgproc.approxPolyDP(contour2f, approx, epsilon, true)
            
            // Convert to list of points
            val points = approx.toArray().toList()
            
            // Only keep contours with at least 3 points (triangles or more complex)
            if (points.size >= 3) {
                simplifiedContours.add(points)
            }
            
            contour2f.release()
            approx.release()
        }
        
        Log.d(TAG, "Simplified to ${simplifiedContours.size} polygons")
        
        // Clean up
        mat.release()
        grayMat.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        return simplifiedContours
    }

    /**
     * Data class to hold vectorization result
     */
    data class VectorizedImage(
        val contours: List<List<Point>>,
        val width: Int,
        val height: Int,
        val previewBitmap: Bitmap
    )

    /**
     * Complete vectorization pipeline
     */
    fun vectorizeImage(bitmap: Bitmap, thresholdValue: Int = 128, epsilon: Double = 2.0): VectorizedImage {
        val processedBitmap = preprocessForVectorization(bitmap, thresholdValue)
        val contours = extractContours(processedBitmap, epsilon)
        
        return VectorizedImage(
            contours = contours,
            width = bitmap.width,
            height = bitmap.height,
            previewBitmap = processedBitmap
        )
    }
}
