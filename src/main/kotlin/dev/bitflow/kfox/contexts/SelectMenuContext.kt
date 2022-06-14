package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import java.util.*

open class SelectMenuContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, bundles, registry)

class PublicSelectMenuContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(client, bundles, event, registry)

class EphemeralSelectMenuContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(client, bundles, event, registry)
