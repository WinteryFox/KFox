package dev.kfox.contexts

import dev.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent

sealed class ButtonContext(
    client: Kord,
    override val response: InteractionResponseBehavior,
    @Suppress("unused")
    val event: ButtonInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : ComponentContext(client, response, componentRegistry)

class PublicButtonContext(
    client: Kord,
    override val response: PublicMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : ButtonContext(client, response, event, componentRegistry)

class EphemeralButtonContext(
    client: Kord,
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : ButtonContext(client, response, event, componentRegistry)
