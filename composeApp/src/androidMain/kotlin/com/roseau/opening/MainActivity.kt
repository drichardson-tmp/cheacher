package com.roseau.opening

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.roseau.opening.progress.AndroidDataStoreLocation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidDataStoreLocation.filesDirPath = applicationContext.filesDir.absolutePath
        setContent {
            App()
        }
    }
}
