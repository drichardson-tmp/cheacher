package com.cheacher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cheacher.app.engine.AndroidEngineLocation
import com.cheacher.app.progress.AndroidDataStoreLocation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidDataStoreLocation.filesDirPath = applicationContext.filesDir.absolutePath
        AndroidEngineLocation.nativeLibraryDir = applicationInfo.nativeLibraryDir
        setContent {
            App()
        }
    }
}
