package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import java.util.*

open class ButtonContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, bundles, registry)

class PublicButtonContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(client, bundles, event, registry)

class EphemeralButtonContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ButtonInteractionCreateEvent,
    registry: ComponentRegistry
) : ButtonContext(client, bundles, event, registry)
