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
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(kord, kfox, event, bundles, registry)

class PublicButtonContext(
    kord: Kord,
    kfox: KFox,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(kord, kfox, bundles, event, registry)

class EphemeralButtonContext(
    kord: Kord,
    kfox: KFox,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(kord, kfox, bundles, event, registry)
