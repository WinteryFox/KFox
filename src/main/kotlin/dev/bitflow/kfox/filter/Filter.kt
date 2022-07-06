package dev.bitflow.kfox.filter

import dev.bitflow.kfox.context.Context

interface Filter {
    suspend fun <T> doFilter(context: Context<T>): Boolean
}
