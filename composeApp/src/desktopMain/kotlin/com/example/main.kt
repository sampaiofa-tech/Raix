package com.example

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val isDev = System.getProperty("raix.dev") == "true" || System.getenv("RAIX_DEV") == "true"
    val windowTitle = if (isDev) "Raix [desktop-dev]" else "Raix"
    
    val windowState = rememberWindowState(size = DpSize(450.dp, 800.dp))
    
    Window(
        onCloseRequest = ::exitApplication,
        title = windowTitle,
        state = windowState,
        icon = painterResource("icon.png"),
        alwaysOnTop = true
    ) {
        App()
    }
}
