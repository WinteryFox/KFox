package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.bitflow.kfox.KFox
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ApplicationCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.interaction.ModalBuilder
import java.lang.NullPointerException
import java.text.MessageFormat
import java.util.*

@Suppress("unused")
suspend inline fun ButtonBuilder.InteractionButtonBuilder.register(registry: ComponentRegistry, callbackId: String) {
    registry.save(customId, callbackId)
}

@Suppress("unused")
suspend inline fun SelectMenuBuilder.register(registry: ComponentRegistry, callbackId: String) {
    registry.save(customId, callbackId)
}

@Suppress("unused")
suspend inline fun ModalBuilder.register(registry: ComponentRegistry, callbackId: String) {
    registry.save(customId, callbackId)
}

sealed class Context(
    val kord: Kord,
    val kfox: KFox,
    open val event: InteractionCreateEvent,
    val bundles: Map<Locale, ResourceBundle>,
    private val registry: ComponentRegistry
) {
    @Suppress("unused")
    suspend fun ButtonBuilder.InteractionButtonBuilder.register(callbackId: String) {
        register(registry, callbackId)
    }

    @Suppress("unused")
    suspend fun SelectMenuBuilder.register(callbackId: String) {
        register(registry, callbackId)
    }

    @Suppress("unused")
    suspend fun ModalBuilder.register(callbackId: String) {
        register(registry, callbackId)
    }

    fun supportsLocale(locale: Locale) = bundles.containsKey(locale)

    fun getString(key: String, locale: Locale?, vararg params: List<Any>): String {
        val bundle = bundles[locale]
            ?: bundles[kfox.defaultLocale]
        val string = if (bundle == null) key else try {
            bundle.getString(key)
        } catch (_: NullPointerException) {
            key
        }
        return MessageFormat.format(string, params)
    }

    fun getUserString(key: String, vararg params: List<Any>): String =
        getString(key, event.interaction.locale, *params)

    fun getGuildString(key: String, vararg params: List<Any>): String =
        getString(key, event.interaction.guildLocale, *params)
}

sealed class CommandContext(
    kord: Kord,
    kfox: KFox,
    override val event: ApplicationCommandInteractionCreateEvent,
    bundles: Map<Locale, ResourceBundle>,
    registry: ComponentRegistry
) : Context(kord, kfox, event, bundles, registry)

sealed class ComponentContext(
    kord: Kord,
    kfox: KFox,
    override val event: ComponentInteractionCreateEvent,
    bundles: Map<Locale, ResourceBundle>,
    registry: ComponentRegistry
) : Context(kord, kfox, event, bundles, registry)

open class ModalContext(
    kord: Kord,
    kfox: KFox,
    bundles: Map<Locale, ResourceBundle>,
    @Suppress("unused")
    override val event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, event, bundles, registry)
