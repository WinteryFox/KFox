package dev.bitflow.kfox.context

import dev.bitflow.kfox.AsKordEvent
import dev.bitflow.kfox.KFox
import dev.bitflow.kfox.data.ComponentRegistry
import dev.kord.common.Locale
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.interaction.response.InteractionResponseBehavior
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
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

sealed class Context<T, E : AsKordEvent<T>>(
    val kord: Kord,
    val kfox: KFox<T, E>,
    @Suppress("MemberVisibilityCanBePrivate") val translationModule: String,
    open val event: InteractionCreateEvent,
    open val source: T,
    open val response: InteractionResponseBehavior?,
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

    @OptIn(KordUnsafe::class, KordExperimental::class)
    fun InteractionCreateEvent.guild(): GuildBehavior? =
        interaction.data.guildId.value?.let { kord.unsafe.guild(it) }

    @OptIn(KordUnsafe::class, KordExperimental::class)
    fun InteractionCreateEvent.member(): MemberBehavior? =
        interaction.data.member.value?.let { kord.unsafe.member(it.guildId, it.userId) }

    suspend fun GuildChannel.permissionsForMember(memberId: Snowflake): Permissions = when (this) {
        is TopGuildChannel -> getEffectivePermissions(memberId)
        is ThreadChannel -> getParent().getEffectivePermissions(memberId)
        else -> error("Unsupported channel type for channel: $this")
    }

    suspend fun checkPermissions(permissions: Set<Permission>) {
        if (maskPermissions(permissions).isNotEmpty())
            TODO("Because I am lazy, fuck you user, just kidding I love you <3. P.S. Feel free to PR this code for me <3")
    }

    suspend fun maskPermissions(permissions: Set<Permission>): Set<Permission> {
        if (permissions.isEmpty())
            return emptySet()

        return if (event.guild() != null)
            permissions.filter {
                !event.interaction.getChannel().asChannelOf<GuildChannel>().permissionsForMember(kord.selfId)
                    .contains(it)
            }.toSet()
        else
            emptySet()
    }
}

sealed class CommandContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    override val event: ApplicationCommandInteractionCreateEvent,
    override val source: T,
    response: InteractionResponseBehavior?,
    registry: ComponentRegistry
) : Context<T, E>(kord, kfox, translationModule, event, source, response, registry)

sealed class ComponentContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    override val event: ComponentInteractionCreateEvent,
    override val source: T,
    response: InteractionResponseBehavior?,
    registry: ComponentRegistry
) : Context<T, E>(kord, kfox, translationModule, event, source, response, registry)

open class ModalContext<T, E : AsKordEvent<T>>(
    kord: Kord,
    kfox: KFox<T, E>,
    translationModule: String,
    @Suppress("unused")
    override val event: ModalSubmitInteractionCreateEvent,
    override val source: T,
    response: InteractionResponseBehavior?,
    registry: ComponentRegistry
) : Context<T, E>(kord, kfox, translationModule, event, source, response, registry)
