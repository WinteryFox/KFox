package dev.bitflow.kfox.filter

import dev.bitflow.kfox.AsKordEvent
import dev.bitflow.kfox.context.Context

object GuildFilter : Filter {
    override suspend fun <T, E : AsKordEvent<T>> doFilter(context: Context<T, E>): Boolean =
        context.event.interaction.data.guildId.value != null
}
