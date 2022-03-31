package dev.kfox

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

internal data class ParameterData(
    val name: String?,
    val description: String?,
    val parameter: KParameter
)

internal data class CommandNode(
    val name: String,
    val description: String,
    val function: KFunction<*>,
    val parameters: Map<String, ParameterData> = emptyMap(),
    val parent: String? = null,
    val children: List<CommandNode> = emptyList()
)
