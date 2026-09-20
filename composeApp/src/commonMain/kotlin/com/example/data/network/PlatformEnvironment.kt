package com.example.data.network

/**
 * Multiplatform environment abstraction.
 *
 * ANTI-SPOOFING POLICY:
 * In Release builds, [isDebug] is always false and [getEnv] returns null,
 * guaranteeing that production releases cannot be diverted by environment variables.
 */
expect object PlatformEnvironment {
    val isDebug: Boolean
    val webApiKey: String
    fun getEnv(name: String): String?
    fun currentTimeMillis(): Long
    fun getRestHeaders(): Map<String, String>
}
