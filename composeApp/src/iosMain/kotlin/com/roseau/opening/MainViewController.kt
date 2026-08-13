package com.roseau.opening

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName") // Called from Swift.
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
