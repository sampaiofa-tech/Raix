package com.example.security

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File

object MasterPasswordManager {
    private val pinStorageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "master_pin.dpapi")
    }

    fun isMasterPasswordSet(): Boolean {
        return pinStorageFile.exists()
    }

    fun setMasterPassword(pin: String) {
        if (Platform.isWindows()) {
            val protectedBytes = Crypt32Util.cryptProtectData(pin.toByteArray(Charsets.UTF_8))
            pinStorageFile.writeBytes(protectedBytes)
        } else {
            pinStorageFile.writeText(pin)
        }
    }

    fun verifyMasterPassword(pin: String): Boolean {
        if (!pinStorageFile.exists()) return false
        val storedPin = if (Platform.isWindows()) {
            try {
                String(Crypt32Util.cryptUnprotectData(pinStorageFile.readBytes()), Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
        } else {
            pinStorageFile.readText()
        }
        return storedPin == pin
    }
}
