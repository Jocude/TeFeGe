package com.example.vectorscan

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
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
}
