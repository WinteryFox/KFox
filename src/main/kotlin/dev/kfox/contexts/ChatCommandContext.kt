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
    registry: ComponentRegistry
) : CommandContext(client, response, registry)

class PublicChatCommandContext(
    client: Kord,
    override val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(client, response, event, registry)

class EphemeralChatCommandContext(
    client: Kord,
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(client, response, event, registry)
