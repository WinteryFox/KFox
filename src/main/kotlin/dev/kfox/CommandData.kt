package dev.kfox

import dev.kord.common.entity.Snowflake
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

internal data class ParameterData(
    val name: String?,
    val descriptionKey: String?,
    val parameter: KParameter
)

internal data class CommandNode(
    val name: String,
    val descriptionKey: String,
    //val category: Category,
    val ephemeral: Boolean,
    val executor: KFunction<*>,
    val applicationIds: List<Snowflake>,
    val parameters: Map<String, ParameterData> = emptyMap(),
    val parent: String? = null,
    val children: List<CommandNode> = emptyList()
)
