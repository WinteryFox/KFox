package dev.bitflow.kfox.filter

import dev.bitflow.kfox.context.*

interface Filter {
    suspend fun doFilter(context: Context): Boolean
}
