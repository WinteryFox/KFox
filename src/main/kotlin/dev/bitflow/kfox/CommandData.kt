package dev.bitflow.kfox

import dev.kord.common.entity.Snowflake
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class ParameterData(
    val defaultName: String?,
    val defaultDescription: String?,
    val nameKey: String?,
    val descriptionKey: String?,
    val parameter: KParameter
)

data class CommandData(
    val defaultName: String,
    val defaultDescription: String,
    val nameKey: String,
    val descriptionKey: String,
    val function: KFunction<*>,
    val parameters: Map<String, ParameterData>,
    val guild: Snowflake?,
    val group: GroupData?,
    val parent: String?,
    val children: List<CommandData>
)

data class GroupData(
    val defaultName: String,
    val defaultDescription: String,
    val nameKey: String,
    val descriptionKey: String,
)
