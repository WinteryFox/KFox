package dev.bitflow.kfox.context

import dev.bitflow.kfox.KFox
import dev.bitflow.kfox.data.ComponentRegistry
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

open class SelectMenuContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    event: SelectMenuInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : ComponentContext<T>(kord, kfox, translationModule, event, source, null, registry)

class PublicSelectMenuContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    override val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : SelectMenuContext<T>(kord, kfox, translationModule, event, source, registry)

class EphemeralSelectMenuContext<T>(
    kord: Kord,
    kfox: KFox<T, *>,
    translationModule: String,
    @Suppress("unused")
    override val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    source: T,
    registry: ComponentRegistry
) : SelectMenuContext<T>(kord, kfox, translationModule, event, source, registry)
