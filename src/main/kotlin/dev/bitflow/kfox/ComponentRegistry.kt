package dev.bitflow.kfox

interface ComponentRegistry {
    suspend fun save(buttonId: String, callbackId: String)

    suspend fun get(buttonId: String): String?
}
