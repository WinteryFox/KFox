package dev.bitflow.kfox.context

import dev.bitflow.kfox.AsKordEvent
import dev.bitflow.kfox.KFox
import dev.bitflow.kfox.data.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

open class SelectMenuContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    event: SelectMenuInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ComponentContext<T, E>(kord, kfox, translationModule, event, source, null, registry)

class PublicSelectMenuContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : SelectMenuContext<T, E>(kord, kfox, translationModule, event, source, registry)

class EphemeralSelectMenuContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : SelectMenuContext<T, E>(kord, kfox, translationModule, event, source, registry)
