package dev.bitflow.kfox

import dev.bitflow.kfox.contexts.*
import dev.bitflow.kfox.localization.TranslationProvider
import dev.kord.common.Locale
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
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
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

class KFox(
    reflections: Reflections,
    val translation: TranslationProvider,
    private val registry: ComponentRegistry = MemoryComponentRegistry(),
    eventsFlow: (KFox) -> SharedFlow<Event>
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val events: SharedFlow<Event> = eventsFlow(this)
    private val logger = KotlinLogging.logger {}
    private val commands: Map<String, CommandData>
    private val localComponentCallbacks: Map<String, ComponentCallback>

    init {
        commands = scanForCommands(translation, reflections).associateBy { it.defaultName }
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

    internal suspend fun putCommands(kord: Kord): KFox {
        val globalCommands = commands.values.filter { it.guild == null }
        if (globalCommands.isNotEmpty())
            kord.createGlobalApplicationCommands {
                registerCommands(globalCommands, translation)
            }.collect()

        val guildCommands = commands.values.filter { it.guild != null }
        if (guildCommands.isNotEmpty())
            guildCommands
                .groupBy { it.guild!! }
                .map {
                    kord.createGuildApplicationCommands(it.key) {
                        registerCommands(
                            it.value,
                            translation
                        )
                    }
                }
                .merge()
                .collect()

        return this
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
                        event as ChatInputCommandInteractionCreateEvent,
                        registry
                    )

                PublicChatCommandContext::class ->
                    PublicChatCommandContext(
                        kord,
                        kfox,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralChatCommandContext::class ->
                    EphemeralChatCommandContext(
                        kord,
                        kfox,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                ButtonContext::class ->
                    ButtonContext(
                        kord,
                        kfox,
                        event as ButtonInteractionCreateEvent,
                        registry
                    )

                PublicButtonContext::class ->
                    PublicButtonContext(
                        kord,
                        kfox,
                        (event as ButtonInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralButtonContext::class ->
                    EphemeralButtonContext(
                        kord,
                        kfox,
                        (event as ButtonInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                SelectMenuContext::class ->
                    SelectMenuContext(
                        kord,
                        kfox,
                        event as SelectMenuInteractionCreateEvent,
                        registry
                    )

                PublicSelectMenuContext::class ->
                    PublicSelectMenuContext(
                        kord,
                        kfox,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralSelectMenuContext::class ->
                    EphemeralSelectMenuContext(
                        kord,
                        kfox,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        registry
                    )

                ModalContext::class ->
                    ModalContext(
                        kord,
                        kfox,
                        event as ModalSubmitInteractionCreateEvent,
                        registry
                    )

                PublicModalContext::class ->
                    PublicModalContext(
                        kord,
                        kfox,
                        (event as ModalSubmitInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        registry
                    )

                EphemeralModalContext::class ->
                    EphemeralModalContext(
                        kord,
                        kfox,
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

fun scanForCommands(translationProvider: TranslationProvider, reflections: Reflections): List<CommandData> {
    val localCommands = reflections.getMethodsAnnotatedWith(Command::class.java)
        .map { it.kotlinFunction!! }
        .map { function ->
            val annotation = function.findAnnotation<Command>()!!
            val subCommand = function.findAnnotation<SubCommand>()
            val group = function.findAnnotation<Group>()

            CommandData(
                translationProvider.getString(annotation.nameKey, locale = translationProvider.defaultLocale),
                translationProvider.getString(annotation.descriptionKey, locale = translationProvider.defaultLocale),
                annotation.nameKey,
                annotation.descriptionKey,
                function,
                function.parameters.map {
                    val p = it.findAnnotation<Parameter>()
                    ParameterData(
                        if (p == null) null else translationProvider.getString(
                            p.nameKey,
                            locale = translationProvider.defaultLocale
                        ),
                        if (p == null) null else translationProvider.getString(
                            p.descriptionKey,
                            locale = translationProvider.defaultLocale
                        ),
                        p?.nameKey,
                        p?.descriptionKey,
                        it
                    )
                }.associateBy { it.parameter.name!! },
                if (annotation.guild != Long.MIN_VALUE) Snowflake(annotation.guild) else null,
                if (group == null) null else GroupData(
                    translationProvider.getString(group.nameKey, locale = translationProvider.defaultLocale),
                    translationProvider.getString(group.descriptionKey, locale = translationProvider.defaultLocale),
                    group.nameKey,
                    group.descriptionKey
                ),
                if (subCommand == null) null else translationProvider.getString(
                    subCommand.parentNameKey,
                    locale = translationProvider.defaultLocale
                ),
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
    translationProvider: TranslationProvider
) {
    fun getLocalization(key: String): MutableMap<Locale, String> =
        translationProvider.getAllStrings(key).toMutableMap()

    fun SubCommandBuilder.addSubCommand(command: CommandData) {
        nameLocalizations = getLocalization(command.nameKey)
        descriptionLocalizations = getLocalization(command.descriptionKey)
        addParameters(translationProvider, command)
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

            addParameters(translationProvider, command)
        }
    }
}

private fun BaseInputChatBuilder.addParameters(translationProvider: TranslationProvider, command: CommandData) {
    fun OptionsBuilder.localize(parameter: ParameterData) {
        nameLocalizations = translationProvider.getAllStrings(parameter.nameKey!!).toMutableMap()
        descriptionLocalizations = translationProvider.getAllStrings(parameter.descriptionKey!!).toMutableMap()
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
    translationProvider: TranslationProvider,
    registry: ComponentRegistry = MemoryComponentRegistry(),
    registerCommands: Boolean = true
): KFox = if (registerCommands) {
    KFox(
        Reflections(ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`)),
        translationProvider,
        registry
    ) { events }.putCommands(this)
} else {
    KFox(
        Reflections(ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`)),
        translationProvider,
        registry
    ) { events }
}

