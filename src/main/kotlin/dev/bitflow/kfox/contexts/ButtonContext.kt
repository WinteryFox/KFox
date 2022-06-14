package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import java.util.*

open class ButtonContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(kord, kfox, event, registry)

class PublicButtonContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(kord, kfox, event, registry)

class EphemeralButtonContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(kord, kfox, event, registry)
