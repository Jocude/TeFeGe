package com.example.vectorscan

import android.content.Context
import android.util.Log
import org.opencv.core.Point
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object DxfExporter {
    private const val TAG = "DxfExporter"

    /**
     * Export contours to DXF file format (AutoCAD compatible)
     * @param context Android context for file access
     * @param contours List of contours (each is a list of points)
     * @param width Original image width (for coordinate scaling)
     * @param height Original image height (for coordinate scaling)
     * @param fileName Optional custom filename
     * @return File object of created DXF file
     */
    fun exportToDxf(
        context: Context,
        contours: List<List<Point>>,
        width: Int,
        height: Int,
        fileName: String? = null
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dxfFileName = fileName ?: "vectorscan_$timestamp.dxf"
        
        val file = File(context.getExternalFilesDir(null), dxfFileName)
        
        FileWriter(file).use { writer ->
            // Write DXF header
            writeDxfHeader(writer)
            
            // Write entities section
            writer.write("0\nSECTION\n2\nENTITIES\n")
            
            // Write each contour as a LWPOLYLINE
            contours.forEachIndexed { index, contour ->
                writeLwPolyline(writer, contour, index, height)
            }
            
            // End entities section
            writer.write("0\nENDSEC\n")
            
            // Write EOF
            writer.write("0\nEOF\n")
        }
        
        Log.d(TAG, "DXF file created: ${file.absolutePath}")
        Log.d(TAG, "Exported ${contours.size} contours")
        
        return file
    }

    private fun writeDxfHeader(writer: FileWriter) {
        writer.write("""
            |999
            |DXF created by VectorScan
            |0
            |SECTION
            |2
            |HEADER
            |9
            |${'$'}ACADVER
            |1
            |AC1015
            |0
            |ENDSEC
        """.trimMargin())
        writer.write("\n")
    }

    private fun writeLwPolyline(writer: FileWriter, points: List<Point>, layerId: Int, imageHeight: Int) {
        if (points.isEmpty()) return
        
        writer.write("0\nLWPOLYLINE\n")
        writer.write("8\n")  // Layer code
        writer.write("$layerId\n")  // Layer number
        writer.write("90\n")  // Number of vertices
        writer.write("${points.size}\n")
        writer.write("70\n")  // Closed flag
        writer.write("1\n")  // 1 = closed polyline
        
        // Write each vertex
        for (point in points) {
            // Flip Y coordinate because DXF uses bottom-left origin while images use top-left
            val x = point.x
            val y = imageHeight - point.y
            
            writer.write("10\n")  // X coordinate code
            writer.write("$x\n")
            writer.write("20\n")  // Y coordinate code
            writer.write("$y\n")
        }
    }

    /**
     * Export contours to SVG file format (web/vector graphics compatible)
     */
    fun exportToSvg(
        context: Context,
        contours: List<List<Point>>,
        width: Int,
        height: Int,
        fileName: String? = null
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val svgFileName = fileName ?: "vectorscan_$timestamp.svg"
        
        val file = File(context.getExternalFilesDir(null), svgFileName)
        
        FileWriter(file).use { writer ->
            // SVG header
            writer.write("""
                |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
                |<svg width="$width" height="$height" xmlns="http://www.w3.org/2000/svg">
            """.trimMargin())
            writer.write("\n")
            
            // Write each contour as a polygon
            for (contour in contours) {
                if (contour.isEmpty()) continue
                
                writer.write("  <polygon points=\"")
                contour.forEach { point ->
                    writer.write("${point.x},${point.y} ")
                }
                writer.write("\" style=\"fill:none;stroke:black;stroke-width:1\" />\n")
            }
            
            writer.write("</svg>\n")
        }
        
        Log.d(TAG, "SVG file created: ${file.absolutePath}")
        
        return file
    }
}
