package dev.kfox.contexts

import dev.kfox.ComponentRegistry
import dev.kord.core.Kord

sealed class CommandContext(
    val client: Kord,
    val registry: ComponentRegistry
)

sealed class ComponentContext(
    val client: Kord,
    val registry: ComponentRegistry
)
