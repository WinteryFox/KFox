package dev.bitflow.kfox.filter

import dev.bitflow.kfox.AsKordEvent
import dev.bitflow.kfox.context.Context

interface Filter {
    suspend fun <T, E : AsKordEvent<T>> doFilter(context: Context<T, E>): Boolean
}
