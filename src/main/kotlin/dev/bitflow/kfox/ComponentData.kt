package dev.bitflow.kfox

import kotlin.reflect.KFunction

internal data class ComponentCallback(
    val callbackId: String,
    val function: KFunction<*>
)
