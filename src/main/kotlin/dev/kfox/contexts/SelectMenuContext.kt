package dev.kfox.contexts

import dev.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

sealed class SelectMenuContext(
    client: Kord,
    override val response: InteractionResponseBehavior,
    val event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(client, response, registry)

class PublicSelectMenuContext(
    client: Kord,
    override val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(client, response, event, registry)

class EphemeralSelectMenuContext(
    client: Kord,
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(client, response, event, registry)
