package dev.kfox.contexts

import dev.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

open class SelectMenuContext(
    client: Kord,
    override val response: InteractionResponseBehavior,
    val event: SelectMenuInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : ComponentContext(client, response, componentRegistry)

class PublicSelectMenuContext(
    client: Kord,
    override val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : SelectMenuContext(client, response, event, componentRegistry)

class EphemeralSelectMenuContext(
    client: Kord,
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : SelectMenuContext(client, response, event, componentRegistry)
