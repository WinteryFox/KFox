package dev.kfox

import dev.kfox.contexts.*
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.ComponentType
import dev.kord.core.Kord
import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.entity.interaction.GroupCommand
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.core.entity.interaction.StringOptionValue
import dev.kord.core.event.interaction.*
import dev.kord.core.on
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

suspend fun Kord.listen(
    `package`: String,
    applicationCommands: Flow<ApplicationCommand>,
    registry: ComponentRegistry = MemoryComponentRegistry()
): Job {
    val logger = KotlinLogging.logger {}
    val reflections = Reflections(
        ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`)
    )

    val localCommands: MutableList<CommandNode> = mutableListOf()
        reflections.getMethodsAnnotatedWith(Command::class.java)
            .map { it.kotlinFunction!! }
            .sortedBy { if (it.findAnnotation<SubCommand>() == null) 1 else -1 }
            .map { function ->
                val annotation = function.findAnnotation<Command>()!!
                val subCommand = function.findAnnotation<SubCommand>()

                val node = CommandNode(
                    annotation.name,
                    annotation.description,
                    function,
                    function.parameters.map {
                        val p = it.findAnnotation<Parameter>()
                        ParameterData(p?.name, p?.description, it)
                    }.associateBy { it.parameter.name!! },
                    subCommand?.group,
                    subCommand?.parent
                )

                localCommands.add(node)
            }

    val localComponentCallbacks =
        reflections.getMethodsAnnotatedWith(Button::class.java).map { it.kotlinFunction!! }.associate { function ->
            val annotation = function.findAnnotation<Button>()!!

            annotation.callbackId to ComponentCallback(
                annotation.callbackId,
                function,
                ComponentType.Button
            )
        } + reflections.getMethodsAnnotatedWith(SelectMenu::class.java).map { it.kotlinFunction!! }
            .associate { function ->
                val annotation = function.findAnnotation<SelectMenu>()!!

                annotation.callbackId to ComponentCallback(
                    annotation.callbackId,
                    function,
                    ComponentType.SelectMenu
                )
            }

    val commands = applicationCommands.filter { command ->
        val localCommand = localCommands.find { it.name == command.name || it.parent == command.name }

        if (localCommand == null) {
            logger.warn { "Command \"${command.name}\" is not locally defined, skipping." }
            false
        } else {
            true
        }
    }.toList().associate { command ->
        // TODO: Create missing commands with PUT (or however we end up doing it)
        val localCommand = localCommands.filter { it.name == command.name || it.parent == command.name } // TODO: Create missing commands?

        command.id to localCommand
    }.filterValues { it.isNotEmpty() }

    return on<InteractionCreateEvent> {
        when (this) {
            is ComponentInteractionCreateEvent -> {
                val callbackId = registry.get(interaction.componentId)

                if (callbackId == null) {
                    logger.debug { "Callback for component ${interaction.componentId} is not registered, did you forget to call `register` in the builder?" }
                    return@on
                }

                val callback = localComponentCallbacks[callbackId]

                if (callback == null) {
                    logger.debug { "Callback for component ${interaction.componentId} is not defined." }
                    return@on
                }

                callback.function.callSuspendByParameters(
                    kord,
                    registry,
                    this,
                    emptyMap()
                )
            }

            is ChatInputCommandInteractionCreateEvent -> {
                val matchedCommands = commands[interaction.command.rootId]
                    ?: throw IllegalStateException("Bot command is not locally known (${interaction.command.rootId})")

                val localCommand = when (val command = interaction.command) {
                    is dev.kord.core.entity.interaction.SubCommand -> {
                        matchedCommands.firstOrNull { it.parent == command.rootName && it.name == command.name && it.group == null }
                            ?: throw IllegalStateException("Subcommand ${command.rootId} -> ${command.name} is not locally known.")
                    }
                    is GroupCommand -> {
                        matchedCommands.firstOrNull { it.parent == command.rootName && it.name == command.name && it.group == command.groupName }
                            ?: throw IllegalStateException("Subcommand ${command.rootId} -> ${command.name} is not locally known.")
                    }
                    else -> {
                        matchedCommands.first()
                    }
                }

                val suppliedParameters = interaction.command.options.mapValues {
                    when (it.value) {
                        is StringOptionValue -> StringOptionValue((it.value.value as String).trim(), it.value.focused)
                        else -> it.value
                    }
                }

                localCommand.function.callSuspendByParameters(
                    kord,
                    registry,
                    this,
                    suppliedParameters,
                    localCommand.parameters
                )
            }
            else -> TODO()
        }
    }
}

@OptIn(KordUnsafe::class)
internal suspend fun KFunction<*>.callSuspendByParameters(
    kord: Kord,
    registry: ComponentRegistry,
    event: InteractionCreateEvent,
    suppliedParameters: Map<String, OptionValue<Any?>>,
    commandParameters: Map<String, ParameterData> = emptyMap(),
) {
    callSuspendBy(
        parameters.associateWith { parameter ->
            val supplied = suppliedParameters.entries.find {
                it.key == (commandParameters[parameter.name]?.name ?: parameter.name)
            }?.value

            if (supplied != null)
                return@associateWith supplied.value

            when (parameter.type.classifier) {
                PublicChatCommandContext::class ->
                    PublicChatCommandContext(
                        kord,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )
                EphemeralChatCommandContext::class ->
                    EphemeralChatCommandContext(
                        kord,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )
                PublicButtonContext::class ->
                    PublicButtonContext(
                        kord,
                        (event as ButtonInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )
                EphemeralButtonContext::class ->
                    EphemeralButtonContext(
                        kord,
                        (event as ButtonInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )
                PublicSelectMenuContext::class ->
                    PublicSelectMenuContext(
                        kord,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )
                EphemeralSelectMenuContext::class ->
                    EphemeralSelectMenuContext(
                        kord,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )
                else -> if (parameter.isOptional)
                    null
                else
                    throw IllegalArgumentException("Parameter \"${parameter.name}\" is either of unsupported type or null when it was not optional.")
            }
        }
    )
}

@OptIn(ExperimentalContracts::class)
suspend fun Kord.listen(
    `package`: String,
    componentRegistry: ComponentRegistry = MemoryComponentRegistry(),
    builder: suspend (Kord) -> Flow<ApplicationCommand>
): Job {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return listen(`package`, builder(this), componentRegistry)
}

@Suppress("unused")
suspend fun Kord.listen(
    `package`: String, componentRegistry: ComponentRegistry = MemoryComponentRegistry()
) = listen(`package`, componentRegistry) { globalCommands }

context(ButtonBuilder.InteractionButtonBuilder, CommandContext) @Suppress("unused")
suspend fun register(callbackId: String) {
    registry.save(customId, callbackId)
}

context(SelectMenuBuilder, CommandContext) @Suppress("unused")
suspend fun register(callbackId: String) {
    registry.save(customId, callbackId)
}
