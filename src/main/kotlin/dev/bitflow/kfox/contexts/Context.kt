package dev.bitflow.kfox.contexts

import dev.bitflow.kfox.ComponentRegistry
import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.interaction.ModalBuilder
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
    val client: Kord,
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

    fun getString(key: String, locale: Locale, vararg params: List<Any>): String =
        MessageFormat.format(bundles[locale]!!.getString(key), params)
}

sealed class CommandContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    registry: ComponentRegistry
) : Context(client, bundles, registry)

sealed class ComponentContext(
    client: Kord,
    bundles: Map<Locale, ResourceBundle>,
    registry: ComponentRegistry
) : Context(client, bundles, registry)
