package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.interaction.ModalBuilder

sealed class Context(
    val client: Kord,
    val registry: ComponentRegistry
) {
    @Suppress("unused")
    suspend fun ButtonBuilder.InteractionButtonBuilder.register(callbackId: String) {
        registry.save(customId, callbackId)
    }

    @Suppress("unused")
    suspend fun SelectMenuBuilder.register(callbackId: String) {
        registry.save(customId, callbackId)
    }

    @Suppress("unused")
    suspend fun ModalBuilder.register(callbackId: String) {
        registry.save(customId, callbackId)
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
