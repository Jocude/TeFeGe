package com.example.vectorscan

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OpenCV
        if (ImageProcessor.initOpenCV()) {
            android.util.Log.d("MainActivity", "OpenCV loaded successfully from Activity")
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VectorScanApp()
                }
            }
        }
    }
}

@Composable
fun VectorScanApp() {
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    if (capturedImageUri == null) {
        // Show camera screen
        CameraScreen(
            onImageCaptured = { uri ->
                capturedImageUri = uri
            }
        )
    } else {
        // Show processing screen
        ProcessingScreen(
            imageUri = capturedImageUri!!,
            onBack = {
                capturedImageUri = null
            }
        )
    }



}

