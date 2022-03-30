package dev.kfox

import dev.kfox.contexts.*
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.ComponentType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.entity.application.ApplicationCommand
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
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

@OptIn(KordUnsafe::class)
suspend fun Kord.listen(
    `package`: String,
    applicationCommands: Flow<ApplicationCommand>,
    componentRegistry: ComponentRegistry = MemoryComponentRegistry()
): Job {
    val logger = KotlinLogging.logger {}
    val reflections = Reflections(
        ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`)
    )

    val localCommands =
        reflections.getMethodsAnnotatedWith(Command::class.java).map { it.kotlinFunction!! }.map { function ->
            val annotation = function.findAnnotation<Command>()!!
            val parent = function.findAnnotation<SubCommand>()?.parent

            CommandNode(annotation.name,
                annotation.descriptionKey,
                //annotation.category,
                annotation.ephemeral,
                function,
                annotation.applicationIds.map { Snowflake(it) },
                function.parameters.map {
                    val p = it.findAnnotation<Parameter>()
                    ParameterData(p?.name, p?.descriptionKey, it)
                }.associateBy { it.parameter.name!! },
                parent
            )
        }

    val localComponentCallbacks =
        reflections.getMethodsAnnotatedWith(Button::class.java).map { it.kotlinFunction!! }.associate { function ->
            val annotation = function.findAnnotation<Button>()!!

            annotation.callbackId to ComponentCallback(
                annotation.callbackId,
                annotation.ephemeral,
                function,
                ComponentType.Button
            )
        } + reflections.getMethodsAnnotatedWith(SelectMenu::class.java).map { it.kotlinFunction!! }
            .associate { function ->
                val annotation = function.findAnnotation<SelectMenu>()!!

                annotation.callbackId to ComponentCallback(
                    annotation.callbackId,
                    annotation.ephemeral,
                    function,
                    ComponentType.SelectMenu
                )
            }

    val commands = applicationCommands.filter { command ->
        val localCommand = localCommands.find { it.name == command.name }

        if (localCommand == null) {
            logger.warn { "Command \"${command.name}\" is not locally defined, skipping." }
            false
        } else {
            true
        }
    }.toList().associate { command ->
        // TODO: Create missing commands with PUT (or however we end up doing it)
        val localCommand = localCommands.find { it.name == command.name }!! // TODO: Create missing commands?

        command.id to localCommand
    }

    return on<InteractionCreateEvent> {
        when (this) {
            is ComponentInteractionCreateEvent -> {
                val callbackId = componentRegistry.get(interaction.componentId)

                if (callbackId == null) {
                    logger.debug { "Callback for component ${interaction.componentId} is not registered, did you forget to call `register` in the builder?" }
                    return@on
                }

                val callback = localComponentCallbacks[callbackId]

                if (callback == null) {
                    logger.debug { "Callback for component ${interaction.componentId} is not defined." }
                    return@on
                }

                val response = if (callback.ephemeral)
                    interaction.deferEphemeralResponseUnsafe()
                else
                    interaction.deferPublicResponseUnsafe()

                callback.function.callSuspend(
                    when (callback.type) {
                        ComponentType.Button -> {
                            if (callback.ephemeral)
                                EphemeralButtonContext(
                                    kord,
                                    response as EphemeralMessageInteractionResponseBehavior,
                                    this as ButtonInteractionCreateEvent,
                                    componentRegistry
                                )
                            else
                                PublicButtonContext(
                                    kord,
                                    response as PublicMessageInteractionResponseBehavior,
                                    this as ButtonInteractionCreateEvent,
                                    componentRegistry
                                )
                        }

                        ComponentType.SelectMenu -> {
                            if (callback.ephemeral)
                                EphemeralSelectMenuContext(
                                    kord,
                                    response as EphemeralMessageInteractionResponseBehavior,
                                    this as SelectMenuInteractionCreateEvent,
                                    componentRegistry
                                )
                            else
                                PublicSelectMenuContext(
                                    kord,
                                    response as PublicMessageInteractionResponseBehavior,
                                    this as SelectMenuInteractionCreateEvent,
                                    componentRegistry
                                )
                        }

                        else -> error("...?")  // TODO
                    }
                )
            }

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
                val response = if (localCommand.ephemeral) interaction.deferEphemeralResponseUnsafe()
                else interaction.deferPublicResponseUnsafe()

                // (Attempt to) Fill parameters for call of actual command
                localCommand.executor.callSuspendBy(localCommand.parameters.values.associateWith { parameter ->
                    val supplied = suppliedParameters.entries.find { parameter.name == it.key }?.value
                    if (supplied != null) return@associateWith supplied.value

                    when (parameter.parameter.type.classifier) {
                        PublicChatCommandContext::class -> PublicChatCommandContext(
                            kord, response as PublicMessageInteractionResponseBehavior, this, componentRegistry
                        )
                        EphemeralChatCommandContext::class -> EphemeralChatCommandContext(
                            kord,
                            response as EphemeralMessageInteractionResponseBehavior,
                            this,
                            componentRegistry
                        )
                        else -> if (parameter.parameter.isOptional) null else throw IllegalArgumentException()
                    }
                }.mapKeys { it.key.parameter })
            }
            else -> TODO()
        }
    }
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

suspend fun Kord.listen(
    `package`: String, componentRegistry: ComponentRegistry = MemoryComponentRegistry()
) = listen(`package`, componentRegistry) { globalCommands }

context(ButtonBuilder.InteractionButtonBuilder, CommandContext) suspend fun register(callbackId: String) {
    componentRegistry.save(customId, callbackId)
}

context(SelectMenuBuilder, CommandContext) suspend fun register(callbackId: String) {
    componentRegistry.save(customId, callbackId)
}
