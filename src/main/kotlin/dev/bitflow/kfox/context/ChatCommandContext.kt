package dev.bitflow.kfox.context

import dev.bitflow.kfox.AsKordEvent
import dev.bitflow.kfox.KFox
import dev.bitflow.kfox.data.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

open class ChatCommandContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    event: ChatInputCommandInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : CommandContext<T, E>(kord, kfox, translationModule, event, source, null, registry)

class PublicChatCommandContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ChatCommandContext<T, E>(kord, kfox, translationModule, event, source, registry)

class EphemeralChatCommandContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: ChatInputCommandInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ChatCommandContext<T, E>(kord, kfox, translationModule, event, source, registry)
