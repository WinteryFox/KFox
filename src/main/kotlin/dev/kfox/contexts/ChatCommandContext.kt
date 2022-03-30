package dev.kfox.contexts

import dev.kfox.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.MessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

sealed class ChatCommandContext(
    client: Kord,
    override val response: MessageInteractionResponseBehavior,
    @Suppress("unused")
    val event: ChatInputCommandInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : CommandContext(client, response, componentRegistry)

class PublicChatCommandContext(
    client: Kord,
    override val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : ChatCommandContext(client, response, event, componentRegistry)

class EphemeralChatCommandContext(
    client: Kord,
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    componentRegistry: ComponentRegistry
) : ChatCommandContext(client, response, event, componentRegistry)
