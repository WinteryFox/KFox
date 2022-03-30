package dev.kfox

import dev.kord.common.entity.*
import dev.kord.core.Kord
import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.entity.interaction.*
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String,
    val descriptionKey: String,
    //val category: Category,
    val applicationIds: LongArray = [],
    val ephemeral: Boolean = false,
)

@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val parent: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val name: String,
    val descriptionKey: String
)

class CommandContext(
    val event: InteractionCreateEvent,
    val client: Kord
)

data class ParameterData(
    val name: String?,
    val descriptionKey: String?,
    val parameter: KParameter
)

data class CommandNode(
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

suspend fun Kord.listen(`package`: String, applicationCommands: List<ApplicationCommand>): Job {
    val logger = KotlinLogging.logger {}
    val localCommands =
        Reflections(ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`))
            .getMethodsAnnotatedWith(Command::class.java)
            .map { it.kotlinFunction!! }
            .map { function ->
                val annotation = function.findAnnotation<Command>()!!
                val parent = function.findAnnotation<SubCommand>()?.parent

                CommandNode(
                    annotation.name,
                    annotation.descriptionKey,
                    //annotation.category,
                    annotation.ephemeral,
                    function,
                    annotation.applicationIds.map { Snowflake(it) },
                    function.parameters
                        .map {
                            val p = it.findAnnotation<Parameter>()
                            ParameterData(p?.name, p?.descriptionKey, it)
                        }
                        .associateBy { it.parameter.name!! },
                    parent
                )
            }

    val commands = applicationCommands
        .filter { command ->
            val localCommand =
                localCommands.find { it.name == command.name }

            if (localCommand == null) {
                logger.warn { "Command \"${command.name}\" is not locally defined, skipping." }
                false
            } else {
                true
            }
        }
        .associate { command ->
            // TODO: Create missing commands with PUT (or however we end up doing it)
            val localCommand =
                localCommands.find { it.name == command.name }!! // TODO: Create missing commands?

            command.id to localCommand
        }

    return on<InteractionCreateEvent> {
        when (this) {
            is ChatInputCommandInteractionCreateEvent -> {
                val localCommand = commands[interaction.command.rootId]
                    ?: throw IllegalStateException("Bot command is not locally known (${interaction.command.rootId})")

                val suppliedParameters = interaction.command.options.mapValues {
                    when (it.value) {
                        is StringOptionValue -> StringOptionValue((it.value.value as String).trim(), it.value.focused)
                        else -> it.value
                    }
                }

                // Acknowledge we have received, and we have such a command registered and are now processing
                kord.rest.interaction.deferMessage(
                    interaction.id, interaction.token, localCommand.ephemeral
                )

                // (Attempt to) Fill parameters for call of actual command
                localCommand.executor.callSuspendBy(localCommand.parameters.values
                    .associateWith { parameter ->
                        //val supplied = suppliedParameters.find { parameter.name == it.name }?.value
                        val supplied = suppliedParameters.entries.find { parameter.name == it.key }?.value
                        if (supplied != null)
                            return@associateWith supplied.value

                        when (parameter.parameter.type.classifier) {
                            CommandContext::class -> CommandContext(this, kord)
                            else -> if (parameter.parameter.isOptional) null else throw IllegalArgumentException()
                        }
                    }.mapKeys { it.key.parameter })
            }
            else -> TODO()
        }
    }
}

suspend fun Kord.listen(`package`: String, builder: suspend (Kord) -> List<ApplicationCommand>) =
    listen(`package`, builder(this))

suspend fun Kord.listen(`package`: String) =
    listen(`package`) { globalCommands.toList() }
