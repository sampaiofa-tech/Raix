package com.example.ui.screens

import androidx.compose.runtime.Composable

@Composable
expect fun AppLockGate(
    onUnlocked: () -> Unit
)
