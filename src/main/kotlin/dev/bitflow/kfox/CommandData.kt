package dev.bitflow.kfox

import dev.kord.common.entity.Choice
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class ParameterData(
    val name: String?,
    val description: String?,
    val parameter: KParameter,
    val choices: MutableList<Choice<*>>?=null
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
