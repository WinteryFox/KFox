package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent

open class ModalContext(
    client: Kord,
    @Suppress("unused")
    val event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, registry)

class PublicModalContext(
    client: Kord,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(client, event, registry)

class EphemeralModalContext(
    client: Kord,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(client, event, registry)
