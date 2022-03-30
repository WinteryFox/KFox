package dev.kfox.contexts

import dev.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.InteractionResponseBehavior

open class CommandContext(
    val client: Kord, open val response: InteractionResponseBehavior, val componentRegistry: ComponentRegistry
)

open class ComponentContext(
    val client: Kord,
    open val response: InteractionResponseBehavior,
    val componentRegistry: ComponentRegistry
)
