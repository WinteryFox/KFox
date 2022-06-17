package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.data.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

open class SelectMenuContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(kord, kfox, translationModule, event, registry)

class PublicSelectMenuContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(kord, kfox, translationModule, event, registry)

class EphemeralSelectMenuContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(kord, kfox, translationModule, event, registry)
