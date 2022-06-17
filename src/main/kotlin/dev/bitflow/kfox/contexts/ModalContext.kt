package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.data.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent

class PublicModalContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(kord, kfox, translationModule, event, registry)

class EphemeralModalContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : ModalContext(kord, kfox, translationModule, event, registry)
