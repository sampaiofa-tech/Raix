package com.example.util

import android.content.Context
import android.util.Log

object VersionMigrationManager {

    private const val PREFS_NAME = "raix_version_prefs"
    private const val KEY_LAST_VERSION = "last_version_code"

    fun checkAndWipeOnUpdate(context: Context, currentVersionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastVersionCode = prefs.getInt(KEY_LAST_VERSION, -1)

        if (lastVersionCode != -1 && currentVersionCode > lastVersionCode) {
            Log.w("VersionMigration", "Atualização detectada de $lastVersionCode para $currentVersionCode. Executando Wipe Master...")
            
            // Wipe SharedPreferences
            wipeSharedPreferences(context)
            
            // Wipe Databases (Room)
            wipeDatabases(context)
        }

        // Atualizar versão
        prefs.edit().putInt(KEY_LAST_VERSION, currentVersionCode).apply()
    }

    private fun wipeSharedPreferences(context: Context) {
        val dir = java.io.File(context.applicationInfo.dataDir, "shared_prefs")
        if (dir.exists() && dir.isDirectory) {
            for (file in dir.listFiles() ?: emptyArray()) {
                if (file.name != "$PREFS_NAME.xml") {
                    context.getSharedPreferences(file.nameWithoutExtension, Context.MODE_PRIVATE).edit().clear().commit()
                }
            }
        }
    }

    private fun wipeDatabases(context: Context) {
        val databasesDir = java.io.File(context.applicationInfo.dataDir, "databases")
        if (databasesDir.exists() && databasesDir.isDirectory) {
            databasesDir.listFiles()?.forEach { it.delete() }
        }
    }
}
