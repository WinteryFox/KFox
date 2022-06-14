package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import java.util.*

open class ChatCommandContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : CommandContext(kord, kfox, event, registry)

class PublicChatCommandContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(kord, kfox, event, registry)

class EphemeralChatCommandContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(kord, kfox, event, registry)
