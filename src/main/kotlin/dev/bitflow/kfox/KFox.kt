package dev.bitflow.kfox

import dev.bitflow.kfox.contexts.*
import dev.kord.common.Locale
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import mu.KLogger
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

class KFox(
    reflections: Reflections,
    private val commands: Map<String, CommandData>,
    val defaultLocale: Locale = Locale.ENGLISH_UNITED_STATES,
    private val bundles: Map<Locale, ResourceBundle>,
    private val registry: ComponentRegistry = MemoryComponentRegistry(),
    eventsFlow: (KFox) -> SharedFlow<Event>
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val events: SharedFlow<Event> = eventsFlow(this)
    private val logger = KotlinLogging.logger {}
    private val localComponentCallbacks: Map<String, ComponentCallback>

    init {
        localComponentCallbacks =
            reflections.getMethodsAnnotatedWith(Button::class.java).map { it.kotlinFunction!! }
                .associate { function ->
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
        logger.debug { "Reflection found ${localComponentCallbacks.size} component callbacks." }
        logger.debug { "Serving ${commands.size} commands." }
        logger.info { "KFox instance is ready!" }
    }

    @Suppress("unused")
    fun listen(): Job =
        events.buffer(Channel.UNLIMITED)
            .filterIsInstance<InteractionCreateEvent>()
            .onEach { logger.debug { "Received interaction ${it.interaction.id}" } }
            .onEach { event ->
                launch(event.coroutineContext) {
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
                                        this@KFox,
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
                                        this@KFox,
                                        registry,
                                        this,
                                        emptyMap()
                                    )
                                }

                                is ChatInputCommandInteractionCreateEvent -> {
                                    val matchedCommand = commands[interaction.command.rootName]
                                        ?: throw IllegalStateException("Bot command is not locally known \"${interaction.command.rootName}\" (${interaction.command.rootId}).")

                                    val localCommand = when (val command = interaction.command) {
                                        is dev.kord.core.entity.interaction.SubCommand -> matchedCommand.children.firstOrNull { it.defaultName == command.name && it.group == null }
                                            ?: throw IllegalStateException("Subcommand ${command.rootName} -> ${command.name} is not locally known (${command.rootId}).")

                                        is GroupCommand -> matchedCommand.children.firstOrNull { it.defaultName == command.name && it.group?.defaultName == command.groupName }
                                            ?: throw IllegalStateException("Subcommand ${command.rootName} -> ${command.name} is not locally known (${command.rootId}).")

                                        else -> matchedCommand
                                    }

                                    val suppliedParameters = interaction.command.options.mapValues { it.value }

                                    localCommand.function.callSuspendByParameters(
                                        kord,
                                        this@KFox,
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
            .onStart { logger.info { "Started listening for interactions." } }
            .onCompletion { logger.info { "Stopped listening for interactions." } }
            .launchIn(this)

    @OptIn(KordUnsafe::class)
    private suspend fun KFunction<*>.callSuspendByParameters(
        kord: Kord,
        kfox: KFox,
        registry: ComponentRegistry,
        event: InteractionCreateEvent,
        suppliedParameters: Map<String, Any?>,
        commandParameters: Map<String, ParameterData> = emptyMap(),
    ) {
        val parameters = parameters.associateWith { parameter ->
            val supplied = suppliedParameters
                .entries
                .find {
                    it.key == (commandParameters[parameter.name]?.defaultName ?: parameter.name)
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
            } else if (parameter.type.isMarkedNullable) {
                return@associateWith null
            }

            when (parameter.type.classifier) {
                ChatCommandContext::class ->
                    ChatCommandContext(
                        kord,
                        kfox,
                        bundles,
                        event as ChatInputCommandInteractionCreateEvent,
                        registry
                    )

                PublicChatCommandContext::class ->
                    PublicChatCommandContext(
                        kord,
                        kfox,
                        bundles,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralChatCommandContext::class ->
                    EphemeralChatCommandContext(
                        kord,
                        kfox,
                        bundles,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                ButtonContext::class ->
                    ButtonContext(
                        kord,
                        kfox,
                        bundles,
                        event as ButtonInteractionCreateEvent,
                        registry
                    )

                PublicButtonContext::class ->
                    PublicButtonContext(
                        kord,
                        kfox,
                        bundles,
                        (event as ButtonInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralButtonContext::class ->
                    EphemeralButtonContext(
                        kord,
                        kfox,
                        bundles,
                        (event as ButtonInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                SelectMenuContext::class ->
                    SelectMenuContext(
                        kord,
                        kfox,
                        bundles,
                        event as SelectMenuInteractionCreateEvent,
                        registry
                    )

                PublicSelectMenuContext::class ->
                    PublicSelectMenuContext(
                        kord,
                        kfox,
                        bundles,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralSelectMenuContext::class ->
                    EphemeralSelectMenuContext(
                        kord,
                        kfox,
                        bundles,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                ModalContext::class ->
                    ModalContext(
                        kord,
                        kfox,
                        bundles,
                        event as ModalSubmitInteractionCreateEvent,
                        registry
                    )

                PublicModalContext::class ->
                    PublicModalContext(
                        kord,
                        kfox,
                        bundles,
                        (event as ModalSubmitInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralModalContext::class ->
                    EphemeralModalContext(
                        kord,
                        kfox,
                        bundles,
                        (event as ModalSubmitInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                else -> throw IllegalArgumentException("Failed to wire parameter \"${parameter.name}\".")
            }
        }
        callSuspendBy(
            parameters
        )
    }

    private suspend fun getCallback(
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
}

fun scanForCommands(defaultBundle: ResourceBundle, reflections: Reflections): List<CommandData> {
    val localCommands = reflections.getMethodsAnnotatedWith(Command::class.java)
        .map { it.kotlinFunction!! }
        .map { function ->
            val annotation = function.findAnnotation<Command>()!!
            val subCommand = function.findAnnotation<SubCommand>()
            val group = function.findAnnotation<Group>()

            CommandData(
                defaultBundle.getString(annotation.nameKey),
                defaultBundle.getString(annotation.descriptionKey),
                annotation.nameKey,
                annotation.descriptionKey,
                function,
                function.parameters.map {
                    val p = it.findAnnotation<Parameter>()
                    ParameterData(
                        if (p == null) null else defaultBundle.getString(p.nameKey),
                        if (p == null) null else defaultBundle.getString(p.descriptionKey),
                        p?.nameKey,
                        p?.descriptionKey,
                        it
                    )
                }.associateBy { it.parameter.name!! },
                if (annotation.guild != Long.MIN_VALUE) Snowflake(annotation.guild) else null,
                if (group == null) null else GroupData(
                    defaultBundle.getString(group.nameKey),
                    defaultBundle.getString(group.descriptionKey),
                    group.nameKey,
                    group.descriptionKey
                ),
                if (subCommand == null) null else defaultBundle.getString(subCommand.parentNameKey),
                emptyList()
            )
        }

    return localCommands
        .map { c ->
            c.copy(
                children = localCommands.filter { it.parent != null && it.parent == c.defaultName }
            )
        }
}

private fun MultiApplicationCommandBuilder.registerCommands(
    localCommands: List<CommandData>,
    bundles: Map<Locale, ResourceBundle>
) {
    fun getLocalization(key: String): MutableMap<Locale, String> =
        bundles.mapValues { it.value.getString(key) }.toMutableMap()

    fun SubCommandBuilder.addSubCommand(command: CommandData) {
        nameLocalizations = getLocalization(command.nameKey)
        descriptionLocalizations = getLocalization(command.descriptionKey)
        addParameters(bundles, command)
    }

    for (command in localCommands.filter { it.parent == null }) {
        val children = localCommands.filter { it.parent == command.defaultName }
        input(command.defaultName, command.defaultDescription) {
            nameLocalizations = getLocalization(command.nameKey)
            descriptionLocalizations = getLocalization(command.descriptionKey)

            for (child in children) {
                if (child.group != null)
                    group(child.group.defaultName, child.group.defaultDescription) {
                        nameLocalizations = getLocalization(child.group.nameKey)
                        descriptionLocalizations = getLocalization(child.group.descriptionKey)
                        subCommand(child.defaultName, child.defaultDescription) {
                            addSubCommand(child)
                        }
                    }
                else
                    subCommand(child.defaultName, child.defaultDescription) {
                        addSubCommand(child)
                    }
            }

            addParameters(bundles, command)
        }
    }
}

private fun BaseInputChatBuilder.addParameters(localization: Map<Locale, ResourceBundle>, command: CommandData) {
    fun OptionsBuilder.localize(parameter: ParameterData) {
        nameLocalizations = localization.mapValues { it.value.getString(parameter.nameKey) }.toMutableMap()
        descriptionLocalizations =
            localization.mapValues { it.value.getString(parameter.descriptionKey) }.toMutableMap()
    }

    for (parameter in command.parameters) {
        val name = parameter.value.defaultName
        val description = parameter.value.defaultDescription
        if (name == null || description == null)
            continue

        val nullable = parameter.value.parameter.type.isMarkedNullable
        when (parameter.value.parameter.type.classifier) {
            String::class -> string(name, description) {
                localize(parameter.value)
                required = !nullable
            }

            User::class -> user(name, description) {
                localize(parameter.value)
                required = !nullable
            }

            Boolean::class -> boolean(name, description) {
                localize(parameter.value)
                required = !nullable
            }

            Role::class -> role(name, description) {
                localize(parameter.value)
                required = !nullable
            }

            dev.kord.core.entity.channel.Channel::class -> channel(
                name,
                description
            ) {
                localize(parameter.value)
                required = !nullable
            }

            Attachment::class -> attachment(name, description) { required = !nullable }
            else -> throw UnsupportedOperationException("Parameter of type ${parameter.value.parameter.type} is not supported.")
        }
    }
}

suspend fun Kord.kfox(
    `package`: String,
    bundles: Map<Locale, ResourceBundle>,
    defaultLocale: Locale = Locale.ENGLISH_UNITED_STATES,
    registry: ComponentRegistry = MemoryComponentRegistry(),
    registerCommands: Boolean = true
): KFox {
    val reflections = Reflections(ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`))
    val localCommands = scanForCommands(bundles[defaultLocale]!!, reflections)

    suspend fun Flow<ApplicationCommand>.associateCommands(): Map<String, CommandData> = toList()
        .associate { applicationCommand -> applicationCommand.name to localCommands.find { applicationCommand.name == it.defaultName } }
        .filterValues { it != null }
        .mapValues { it.value!! }

    return if (registerCommands) {
        val localGlobal = localCommands.filter { it.guild == null }
        val localGuild = localCommands.filter { it.guild != null }.groupBy { it.guild!! }
        val global: Flow<ApplicationCommand> = if (localGlobal.isEmpty())
            emptyFlow()
        else
            createGlobalApplicationCommands { registerCommands(localGlobal, bundles) }
        val guild: Flow<ApplicationCommand> = if (localGuild.isEmpty())
            emptyFlow()
        else
            localGuild.map { createGuildApplicationCommands(it.key) { registerCommands(it.value, bundles) } }
                .merge()
        KFox(
            reflections,
            merge(global, guild).associateCommands(),
            defaultLocale,
            bundles,
            registry
        ) { events }
    } else {
        // TODO: Doesn't include guild commands
        KFox(
            reflections,
            getGlobalApplicationCommands().associateCommands(),
            defaultLocale,
            bundles,
            registry
        ) { events }
    }
}

