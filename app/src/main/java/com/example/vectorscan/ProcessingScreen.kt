package com.example.vectorscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProcessingScreen(
    imageUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var vectorizedData by remember { mutableStateOf<ImageProcessor.VectorizedImage?>(null) }
    var thresholdValue by remember { mutableStateOf(128f) }
    var isProcessing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    
    // Load image on first composition
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                // Initial processing
                originalBitmap?.let { bitmap ->
                    val result = ImageProcessor.vectorizeImage(bitmap, thresholdValue.toInt())
                    processedBitmap = result.previewBitmap
                    vectorizedData = result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Procesando Imagen",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Image preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator()
                } else {
                    processedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Processed image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Threshold slider
        Text(text = "Umbral de detección: ${thresholdValue.toInt()}")
        Slider(
            value = thresholdValue,
            onValueChange = { thresholdValue = it },
            valueRange = 0f..255f,
            onValueChangeFinished = {
                // Reprocess when user releases slider
                scope.launch {
                    isProcessing = true
                    withContext(Dispatchers.Default) {
                        originalBitmap?.let { bitmap ->
                            val result = ImageProcessor.vectorizeImage(bitmap, thresholdValue.toInt())
                            processedBitmap = result.previewBitmap
                            vectorizedData = result
                        }
                    }
                    isProcessing = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show contour count
        vectorizedData?.let { data ->
            Text(
                text = "Contornos detectados: ${data.contours.size}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Export buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        withContext(Dispatchers.IO) {
                            try {
                                vectorizedData?.let { data ->
                                    val dxfFile = DxfExporter.exportToDxf(
                                        context = context,
                                        contours = data.contours,
                                        width = data.width,
                                        height = data.height
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "DXF guardado: ${dxfFile.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error al exportar DXF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        isExporting = false
                    }
                },
                enabled = !isExporting && vectorizedData != null
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Exportar DXF")
                }
            }
            
            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        withContext(Dispatchers.IO) {
                            try {
                                vectorizedData?.let { data ->
                                    val svgFile = DxfExporter.exportToSvg(
                                        context = context,
                                        contours = data.contours,
                                        width = data.width,
                                        height = data.height
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "SVG guardado: ${svgFile.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error al exportar SVG", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        isExporting = false
                    }
                },
                enabled = !isExporting && vectorizedData != null
            ) {
                Text("Exportar SVG")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Back button
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nueva captura")
        }
    }
}
