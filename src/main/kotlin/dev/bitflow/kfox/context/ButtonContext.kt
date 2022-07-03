package dev.bitflow.kfox.context

import dev.bitflow.kfox.data.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent

open class ButtonContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(kord, kfox, translationModule, event, null, registry)

class PublicButtonContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(kord, kfox, translationModule, event, registry)

class EphemeralButtonContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(kord, kfox, translationModule, event, registry)
