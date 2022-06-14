package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import java.util.*

open class SelectMenuContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : ComponentContext(kord, kfox, event, registry)

class PublicSelectMenuContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    val response: PublicMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(kord, kfox, event, registry)

class EphemeralSelectMenuContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    val response: EphemeralMessageInteractionResponseBehavior,
    event: SelectMenuInteractionCreateEvent,
    registry: ComponentRegistry
) : SelectMenuContext(kord, kfox, event, registry)
