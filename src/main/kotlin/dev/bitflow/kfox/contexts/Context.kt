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

    fun supportsLocale(locale: Locale) = kfox.translation.supportsLocale(locale)

    fun getString(
        key: String,
        vararg params: List<Any>,
        locale: Locale = kfox.translation.defaultLocale,
        module: String = kfox.translation.defaultModule
    ): String = kfox.translation.getString(key, *params, locale = locale, module = module)

    fun getUserString(key: String, vararg params: List<Any>): String =
        getString(key, *params, locale = event.interaction.locale ?: kfox.translation.defaultLocale)

    fun getGuildString(key: String, vararg params: List<Any>): String =
        getString(key, *params, locale = event.interaction.guildLocale ?: kfox.translation.defaultLocale)
}

sealed class CommandContext(
    kord: Kord,
    kfox: KFox,
    override val event: ApplicationCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, event, registry)

sealed class ComponentContext(
    kord: Kord,
    kfox: KFox,
    override val event: ComponentInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, event, registry)

open class ModalContext(
    kord: Kord,
    kfox: KFox,
    @Suppress("unused")
    override val event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, event, registry)
