package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord

sealed class Context(
    val client: Kord,
    val registry: ComponentRegistry
)

sealed class CommandContext(
    client: Kord,
    registry: ComponentRegistry
) : Context(client, registry)

sealed class ComponentContext(
    client: Kord,
    registry: ComponentRegistry
) : Context(client, registry)
