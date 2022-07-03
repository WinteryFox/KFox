package dev.bitflow.kfox.context

import dev.bitflow.kfox.data.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

open class ChatCommandContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : CommandContext(kord, kfox, translationModule, event, null, registry)

class PublicChatCommandContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(kord, kfox, translationModule, event, registry)

class EphemeralChatCommandContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : ChatCommandContext(kord, kfox, translationModule, event, registry)
