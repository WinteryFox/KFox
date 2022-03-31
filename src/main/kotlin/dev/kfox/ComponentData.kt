package dev.kfox

import dev.kord.common.entity.ComponentType
import kotlin.reflect.KFunction

internal data class ComponentCallback(
    val callbackId: String,
    val function: KFunction<*>,
    val type: ComponentType
)
