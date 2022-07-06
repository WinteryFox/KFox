package dev.bitflow.kfox.filter

import dev.bitflow.kfox.context.Context

object GuildFilter : Filter {
    override suspend fun <T> doFilter(context: Context<T>): Boolean =
        context.event.interaction.data.guildId.value != null
}
