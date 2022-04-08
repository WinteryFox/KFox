package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

open class SelectMenuContext(
    client: Kord,
    @Suppress("unused")
    val event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, registry)

class PublicSelectMenuContext(
    client: Kord,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(client, event, registry)

class EphemeralSelectMenuContext(
    client: Kord,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(client, event, registry)
