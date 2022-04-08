package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent

open class ButtonContext(
    client: Kord,
    @Suppress("unused")
    val event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, registry)

class PublicButtonContext(
    client: Kord,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(client, event, registry)

class EphemeralButtonContext(
    client: Kord,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(client, event, registry)
