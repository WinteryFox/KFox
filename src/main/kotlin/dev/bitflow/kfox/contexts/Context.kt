package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.data.ComponentRegistry
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
    @Suppress("MemberVisibilityCanBePrivate") val translationModule: String,
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
        vararg params: Any,
        locale: Locale = kfox.translation.defaultLocale,
        module: String = translationModule
    ): String = kfox.translation.getString(key, *params, locale = locale, module = module)

    fun getUserString(key: String, vararg params: Any, module: String = translationModule): String =
        getString(
            key,
            *params,
            locale = event.interaction.locale ?: kfox.translation.defaultLocale,
            module = module
        )

    fun getGuildString(key: String, vararg params: Any, module: String = translationModule): String =
        getString(
            key,
            *params,
            locale = event.interaction.guildLocale ?: kfox.translation.defaultLocale,
            module = module
        )
}

sealed class CommandContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    override val event: ApplicationCommandInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, translationModule, event, registry)

sealed class ComponentContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    override val event: ComponentInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, translationModule, event, registry)

open class ModalContext(
    kord: Kord,
    kfox: KFox,
    translationModule: String,
    @Suppress("unused")
    override val event: ModalSubmitInteractionCreateEvent,
    registry: ComponentRegistry
) : Context(kord, kfox, translationModule, event, registry)
