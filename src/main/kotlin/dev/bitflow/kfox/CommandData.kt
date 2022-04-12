package dev.bitflow.kfox

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class ParameterData(
    val name: String?,
    val description: String?,
    val parameter: KParameter
)

data class CommandData(
    val name: String,
    val description: String,
    val function: KFunction<*>,
    val parameters: Map<String, ParameterData> = emptyMap(),
    val group: GroupData? = null,
    val parent: String? = null,
    val children: MutableList<CommandData> = mutableListOf(),
)

data class GroupData(
    val name: String,
    val description: String
)
