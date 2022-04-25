package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.interaction.ModalBuilder

@Suppress("unused")
suspend inline fun ButtonBuilder.InteractionButtonBuilder.register(registry: ComponentRegistry, callbackId: String) {
    registry.save(customId, callbackId)
}

@Suppress("unused")
suspend inline fun SelectMenuBuilder.register(registry: ComponentRegistry, callbackId: String) {
    registry.save(customId, callbackId)
}

@Suppress("unused")
suspend inline fun ModalBuilder.register(registry: ComponentRegistry, callbackId: String) {
    registry.save(customId, callbackId)
}

sealed class Context(
    val client: Kord,
    private val registry: ComponentRegistry
) {
    @Suppress("unused")
    suspend fun ButtonBuilder.InteractionButtonBuilder.register(callbackId: String) {
        register(registry, callbackId)
    }

    @Suppress("unused")
    suspend fun SelectMenuBuilder.register(callbackId: String) {
        register(registry, callbackId)
    }

    @Suppress("unused")
    suspend fun ModalBuilder.register(callbackId: String) {
        register(registry, callbackId)
    }
}

sealed class CommandContext(
    client: Kord,
    registry: ComponentRegistry
) : Context(client, registry)

sealed class ComponentContext(
    client: Kord,
    registry: ComponentRegistry
) : Context(client, registry)
