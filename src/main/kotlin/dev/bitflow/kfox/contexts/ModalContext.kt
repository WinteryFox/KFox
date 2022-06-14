package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import java.util.*

class PublicModalContext(
    kord: Kord,
    kfox: KFox,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(kord, kfox, bundles, event, registry)

class EphemeralModalContext(
    kord: Kord,
    kfox: KFox,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(kord, kfox, bundles, event, registry)
