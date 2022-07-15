package dev.bitflow.kfox.context

import dev.bitflow.kfox.KFox
import dev.bitflow.kfox.data.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent

class PublicModalContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ModalContext<T>(kord, kfox, translationModule, event, source, response, registry)

class EphemeralModalContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ModalSubmitInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ModalContext<T>(kord, kfox, translationModule, event, source, response, registry)
