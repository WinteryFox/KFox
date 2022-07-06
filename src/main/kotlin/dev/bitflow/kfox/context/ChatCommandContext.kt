package dev.bitflow.kfox.context

import dev.bitflow.kfox.KFox
import dev.bitflow.kfox.data.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

open class ChatCommandContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    event: ChatInputCommandInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : CommandContext<T>(kord, kfox, translationModule, event, source, null, registry)

class PublicChatCommandContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ChatCommandContext<T>(kord, kfox, translationModule, event, source, registry)

class EphemeralChatCommandContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ChatCommandContext<T>(kord, kfox, translationModule, event, source, registry)
