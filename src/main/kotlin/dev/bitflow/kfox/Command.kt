package dev.bitflow.kfox

import dev.bitflow.kfox.contexts.*
import dev.kord.common.annotation.KordUnsafe
import dev.kord.core.Kord
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.entity.interaction.GroupCommand
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.core.entity.interaction.ResolvableOptionValue
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.*
import dev.kord.core.kordLogger
import dev.kord.rest.builder.interaction.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

internal suspend inline fun getCallback(
    logger: KLogger,
    localComponentCallbacks: Map<String, ComponentCallback>,
    registry: ComponentRegistry,
    id: String
): ComponentCallback? {
    val callbackId = registry.get(id)
    if (callbackId == null) {
        logger.debug { "Callback for component $id is not registered, did you forget to call `register` in the builder?" }
        return null
    }

    val callback = localComponentCallbacks[callbackId]
    if (callback == null) {
        logger.debug { "Callback for component $id is not defined." }
        return null
    }

    return callback
}

fun reflections(`package`: String) = Reflections(
    ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`)
)

suspend fun listen(
    events: Flow<Event>,
    `package`: String,
    applicationCommands: Flow<ApplicationCommand>,
    scope: CoroutineScope,
    registry: ComponentRegistry = MemoryComponentRegistry(),
    reflections: Reflections = reflections(`package`),
    localCommands: List<CommandData> = scanForCommands(reflections)
): Job {
    val logger = KotlinLogging.logger {}

    val localComponentCallbacks =
        reflections.getMethodsAnnotatedWith(Button::class.java).map { it.kotlinFunction!! }.associate { function ->
            val annotation = function.findAnnotation<Button>()!!

            annotation.callbackId to ComponentCallback(
                annotation.callbackId,
                function
            )
        } +
                reflections.getMethodsAnnotatedWith(SelectMenu::class.java).map { it.kotlinFunction!! }
                    .associate { function ->
                        val annotation = function.findAnnotation<SelectMenu>()!!

                        annotation.callbackId to ComponentCallback(
                            annotation.callbackId,
                            function
                        )
                    } +
                reflections.getMethodsAnnotatedWith(Modal::class.java).map { it.kotlinFunction!! }
                    .associate { function ->
                        val annotation = function.findAnnotation<Modal>()!!

                        val params: MutableMap<String, String> = mutableMapOf()

                        for (param in function.parameters) {
                            val name = param.findAnnotation<ModalValue>()?.customId
                                ?: param.name
                                ?: continue

                            params[name] = param.name!!
                        }

                        annotation.callbackId to ModalComponentCallback(
                            annotation.callbackId,
                            function,
                            params
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
        val localCommand =
            localCommands.filter { it.name == command.name || it.parent == command.name } // TODO: Create missing commands?

        command.id to localCommand
    }.filterValues { it.isNotEmpty() }

    return events.buffer(Channel.UNLIMITED)
        .filterIsInstance<InteractionCreateEvent>()
        .onEach { event ->
            scope.launch(event.coroutineContext) {
                runCatching {
                    with(event) {
                        when (this) {
                            is ModalSubmitInteractionCreateEvent -> {
                                val callback = (
                                        getCallback(logger, localComponentCallbacks, registry, interaction.modalId)
                                                as? ModalComponentCallback
                                        ) ?: return@runCatching

                                callback.function.callSuspendByParameters(
                                    kord,
                                    registry,
                                    this,
                                    interaction.textInputs
                                        .filterKeys { it in callback.params }
                                        .map {
                                            callback.params[it.key]!! to it.value.value
                                        }.toMap()
                                )
                            }
                            is ComponentInteractionCreateEvent -> {
                                val callback =
                                    getCallback(logger, localComponentCallbacks, registry, interaction.componentId)
                                        ?: return@runCatching

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
                                        matchedCommands.firstOrNull { it.parent == command.rootName && it.name == command.name && it.group?.name == command.groupName }
                                            ?: throw IllegalStateException("Subcommand ${command.rootId} -> ${command.name} is not locally known.")
                                    }
                                    else -> {
                                        matchedCommands.first()
                                    }
                                }

                                val suppliedParameters = interaction.command.options.mapValues { it.value }

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
                }.onFailure { kordLogger.catching(it) }
            }
        }
        .launchIn(scope)
}

suspend fun Kord.listen(
    `package`: String,
    registry: ComponentRegistry = MemoryComponentRegistry(),
    reflections: Reflections = reflections(`package`),
    localCommands: List<CommandData> = scanForCommands(reflections),
    scope: CoroutineScope = this,
    applicationCommands: suspend Kord.() -> Flow<ApplicationCommand> = {
        createGlobalApplicationCommands {
            registerCommands(localCommands)
        }
    }
): Job = listen(events, `package`, applicationCommands(), scope, registry, reflections, localCommands)

@OptIn(KordUnsafe::class)
internal suspend fun KFunction<*>.callSuspendByParameters(
    kord: Kord,
    registry: ComponentRegistry,
    event: InteractionCreateEvent,
    suppliedParameters: Map<String, Any?>,
    commandParameters: Map<String, ParameterData> = emptyMap(),
) {
    val parameters = parameters.associateWith { parameter ->
        val supplied = suppliedParameters
            .entries
            .find {
                it.key == (commandParameters[parameter.name]?.name ?: parameter.name)
            }

        if (supplied != null) {
            val value = supplied.value
            if (value == null && !parameter.type.isMarkedNullable)
                throw IllegalStateException("Non-nullable parameter \"${supplied.key}\" is null, did you accidentally set not required when creating the command?")
            if (value !is OptionValue<*> || parameter.type.classifier == OptionValue::class.java)
                return@associateWith value

            return@associateWith if (value is ResolvableOptionValue<*>) {
                value.resolvedObject
            } else {
                value.value
            }
        }

        when (parameter.type.classifier) {
            ChatCommandContext::class ->
                ChatCommandContext(
                    kord,
                    event as ChatInputCommandInteractionCreateEvent,
                    registry
                )
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
            ButtonContext::class ->
                ButtonContext(
                    kord,
                    event as ButtonInteractionCreateEvent,
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
            SelectMenuContext::class ->
                SelectMenuContext(
                    kord,
                    event as SelectMenuInteractionCreateEvent,
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
            ModalContext::class ->
                ModalContext(
                    kord,
                    event as ModalSubmitInteractionCreateEvent,
                    registry
                )
            PublicModalContext::class ->
                PublicModalContext(
                    kord,
                    (event as ModalSubmitInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                    event,
                    registry
                )
            EphemeralModalContext::class ->
                EphemeralModalContext(
                    kord,
                    (event as ModalSubmitInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                    event,
                    registry
                )
            else -> if (parameter.isOptional)
                null
            else
                throw IllegalArgumentException("Parameter \"${parameter.name}\" is either of unsupported type or null when it was not optional.")
        }
    }
    callSuspendBy(
        parameters
    )
}

//context(ButtonBuilder.InteractionButtonBuilder, Context) @Suppress("unused")
//suspend fun register(callbackId: String) {
//    registry.save(customId, callbackId)
//}
//
//context(SelectMenuBuilder, Context) @Suppress("unused")
//suspend fun register(callbackId: String) {
//    registry.save(customId, callbackId)
//}
//
//context(ModalBuilder, Context) @Suppress("unused")
//suspend fun register(callbackId: String) {
//    registry.save(customId, callbackId)
//}

fun scanForCommands(reflections: Reflections): List<CommandData> {
    val localCommands = mutableListOf<CommandData>()
    reflections.getMethodsAnnotatedWith(Command::class.java)
        .map { it.kotlinFunction!! }
        .sortedBy { if (it.findAnnotation<SubCommand>() == null) 1 else -1 }
        .map { function ->
            val annotation = function.findAnnotation<Command>()!!
            val subCommand = function.findAnnotation<SubCommand>()
            val group = function.findAnnotation<Group>()

            val node = CommandData(
                annotation.name,
                annotation.description,
                function,
                function.parameters.map {
                    val p = it.findAnnotation<Parameter>()
                    ParameterData(p?.name, p?.description, it)
                }.associateBy { it.parameter.name!! },
                if (group == null) null else GroupData(group.name, group.description),
                if (subCommand?.parent?.isEmpty() == true) null else subCommand?.parent
            )

            localCommands.add(node)
        }
    return localCommands
}

fun MultiApplicationCommandBuilder.registerCommands(localCommands: List<CommandData>) {
    for (command in localCommands.filter { it.parent == null }) {
        val children = localCommands.filter { it.parent == command.name }
        input(command.name, command.description) {
            for (child in children) {
                if (child.group != null)
                    group(child.group.name, child.group.description) {
                        subCommand(child.name, child.description) {
                            addParameters(child)
                        }
                    }
                else
                    subCommand(child.name, child.description) {
                        addParameters(child)
                    }
            }

            addParameters(command)
        }
    }
}

private fun BaseInputChatBuilder.addParameters(command: CommandData) {
    for (parameter in command.parameters) {
        val name = parameter.value.name
        val description = parameter.value.description
        if (name == null || description == null)
            continue

        val nullable = parameter.value.parameter.type.isMarkedNullable
        when (parameter.value.parameter.type.classifier) {
            String::class -> string(name, description) { required = !nullable }
            User::class -> user(name, description) { required = !nullable }
            Boolean::class -> boolean(name, description) { required = !nullable }
            Role::class -> role(name, description) { required = !nullable }
            dev.kord.core.entity.channel.Channel::class -> channel(
                name,
                description
            ) { required = !nullable }
            Attachment::class -> attachment(name, description) { required = !nullable }
            else -> throw UnsupportedOperationException("Parameter of type ${parameter.value.parameter.type} is not supported.")
        }
    }
}
