package dev.bitflow.kfox.filter

import dev.bitflow.kfox.context.Context

object GuildFilter : Filter {
    override suspend fun doFilter(context: Context): Boolean =
        context.event.interaction.data.guildId.value != null
}
