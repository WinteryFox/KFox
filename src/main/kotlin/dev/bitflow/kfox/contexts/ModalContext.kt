package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import java.util.*

open class ModalContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, bundles, registry)

class PublicModalContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(client, bundles, event, registry)

class EphemeralModalContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(client, bundles, event, registry)
