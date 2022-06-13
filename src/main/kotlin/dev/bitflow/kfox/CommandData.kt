package dev.bitflow.kfox

import dev.kord.common.entity.Snowflake
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class ParameterData(
    val name: String?,
    val description: String?,
    val parameter: KParameter
)

data class CommandData(
    val name: String,
    val descriptionKey: String,
    val function: KFunction<*>,
    val parameters: Map<String, ParameterData> = emptyMap(),
    val guild: Snowflake? = null,
    val group: GroupData? = null,
    val parent: String? = null,
    val children: MutableList<CommandData> = mutableListOf(),
)

data class GroupData(
    val name: String,
    val description: String
)
