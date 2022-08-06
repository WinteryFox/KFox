package dev.bitflow.kfox.data

import dev.bitflow.kfox.filter.Filter
import dev.kord.common.entity.Choice
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class ParameterData(
    val defaultName: String?,
    val defaultDescription: String?,
    val nameKey: String?,
    val descriptionKey: String?,
    val translationModule: String?,
    val choices: MutableList<Choice<*>>? = null,
    val parameter: KParameter
)

data class CommandData(
    val defaultName: String,
    val defaultDescription: String,
    val nameKey: String,
    val descriptionKey: String,
    val translationModule: String,
    val function: KFunction<*>,
    val parameters: Map<String, ParameterData>,
    val filters: Set<Filter>,
    val guild: Snowflake?,
    val group: GroupData?,
    val parent: String?,
    val children: List<CommandData>,
    val permissions: Permissions?
)

data class GroupData(
    val defaultName: String,
    val defaultDescription: String,
    val nameKey: String,
    val descriptionKey: String
)

internal open class ComponentCallback(
    val callbackId: String,
    val function: KFunction<*>,
    val translationModule: String
)

internal class ModalComponentCallback(
    callbackId: String,
    function: KFunction<*>,
    val params: Map<String, String>,
    translationModule: String
) : ComponentCallback(callbackId, function, translationModule)
