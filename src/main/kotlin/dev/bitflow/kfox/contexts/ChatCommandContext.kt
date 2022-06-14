package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import java.util.*

open class ChatCommandContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : CommandContext(client, bundles, registry)

class PublicChatCommandContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(client, bundles, event, registry)

class EphemeralChatCommandContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(client, bundles, event, registry)
