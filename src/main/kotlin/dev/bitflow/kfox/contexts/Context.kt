package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord

sealed class CommandContext(
    val client: Kord,
    val registry: ComponentRegistry
)

sealed class ComponentContext(
    val client: Kord,
    val registry: ComponentRegistry
)
