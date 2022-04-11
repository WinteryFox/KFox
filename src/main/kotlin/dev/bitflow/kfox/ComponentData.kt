package dev.bitflow.kfox

import kotlin.reflect.KFunction

internal open class ComponentCallback(
    val callbackId: String,
    val function: KFunction<*>
)

internal class ModalComponentCallback(
    callbackId: String,
    function: KFunction<*>,
    val params: Map<String, String>
) : ComponentCallback(callbackId, function)
