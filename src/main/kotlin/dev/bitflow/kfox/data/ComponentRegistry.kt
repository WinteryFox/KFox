package dev.bitflow.kfox.data

interface ComponentRegistry {
    suspend fun save(buttonId: String, callbackId: String)

    suspend fun get(buttonId: String): String?
}
