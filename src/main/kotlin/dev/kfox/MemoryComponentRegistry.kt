package dev.kfox

object MemoryComponentRegistry : ComponentRegistry {
    private val components = mutableMapOf<String, String>()

    override suspend fun save(buttonId: String, callbackId: String) {
        components[buttonId] = callbackId
    }

    override suspend fun get(buttonId: String): String? = components[buttonId]
}
