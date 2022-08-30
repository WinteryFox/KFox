package dev.bitflow.kfox

import dev.bitflow.kfox.context.*
import dev.bitflow.kfox.data.*
import dev.bitflow.kfox.localization.ResourceBundleTranslationProvider
import dev.bitflow.kfox.localization.TranslationProvider
import dev.kord.common.Locale
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Choice
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.*
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.*
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
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.jvm.kotlinFunction
import dev.kord.common.entity.ChannelType as KordChannelType
import dev.kord.core.entity.channel.Channel as KordChannel

class KFox<T, E : AsKordEvent<T>>(
    `package`: String,
    val translation: TranslationProvider,
    private val registry: ComponentRegistry = MemoryComponentRegistry(),
    eventsFlow: (KFox<T, E>) -> SharedFlow<T>,
    private val mapper: E
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val events: SharedFlow<T> = eventsFlow(this)
    private val logger = KotlinLogging.logger {}
    private val commands: Map<String, CommandData>
    private val localComponentCallbacks: Map<String, ComponentCallback>

    init {
        val reflections =
            Reflections(ConfigurationBuilder().addScanners(Scanners.MethodsAnnotated).forPackage(`package`))
        commands = scanForCommands(translation, reflections).associateBy { it.defaultName }
        localComponentCallbacks =
            reflections.getMethodsAnnotatedWith(Button::class.java).map { it.kotlinFunction!! }
                .associate { function ->
                    val annotation = function.findAnnotation<Button>()!!

                    annotation.callbackId to ComponentCallback(
                        annotation.callbackId,
                        function,
                        function.findAnnotation<Module>()?.module ?: translation.defaultModule
                    )
                } +
                    reflections.getMethodsAnnotatedWith(SelectMenu::class.java).map { it.kotlinFunction!! }
                        .associate { function ->
                            val annotation = function.findAnnotation<SelectMenu>()!!

                            annotation.callbackId to ComponentCallback(
                                annotation.callbackId,
                                function,
                                function.findAnnotation<Module>()?.module ?: translation.defaultModule
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
                                params,
                                function.findAnnotation<Module>()?.module ?: translation.defaultModule
                            )
                        }
        logger.info { "Reflection found ${localComponentCallbacks.size} component callbacks." }
        logger.info { "Serving ${commands.size} commands." }
        logger.info { "KFox instance is ready!" }
    }

    suspend fun putCommands(kord: Kord, snowflake: Snowflake? = null): KFox<T, E> {
        val globalCommands = commands.values.filter { it.guild == null }
        if (globalCommands.isNotEmpty())
            if (snowflake == null)
                kord.createGlobalApplicationCommands {
                    registerCommands(globalCommands, translation)
                }.collect()
            else
                kord.createGuildApplicationCommands(snowflake) {
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
            .map { it to mapper(it) }
            .filter { it.second != null }
            .filter { it.second is InteractionCreateEvent }
            .map { it.first to it.second as InteractionCreateEvent }
            .onEach { logger.trace { "Received interaction ${it.second.interaction.id}" } }
            .onEach { (source, event) ->
                launch {
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
                                        callback.translationModule,
                                        registry,
                                        source,
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
                                        callback.translationModule,
                                        registry,
                                        source,
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
                                        localCommand.translationModule,
                                        registry,
                                        source,
                                        this,
                                        suppliedParameters,
                                        localCommand.parameters,
                                        localCommand.filters
                                    )
                                }

                                else -> TODO()
                            }
                        }
                    }.onFailure {
                        if (it !is KFoxException)
                            logger.catching(it)
                    }
                }
            }
            .onStart { logger.info { "Started listening for interactions." } }
            .onCompletion { logger.info { "Stopped listening for interactions." } }
            .launchIn(this)

    @OptIn(KordUnsafe::class)
    private suspend fun KFunction<*>.callSuspendByParameters(
        kord: Kord,
        kfox: KFox<T, E>,
        translationModule: String,
        registry: ComponentRegistry,
        source: T,
        event: InteractionCreateEvent,
        suppliedParameters: Map<String, Any?>,
        commandParameters: Map<String, ParameterData> = emptyMap(),
        filters: Set<dev.bitflow.kfox.filter.Filter> = emptySet()
    ) {
        var context: Context<T>? = null
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

            context = when (parameter.type.classifier) {
                ChatCommandContext::class ->
                    ChatCommandContext(
                        kord,
                        kfox,
                        translationModule,
                        event as ChatInputCommandInteractionCreateEvent,
                        source,
                        registry
                    )

                PublicChatCommandContext::class ->
                    PublicChatCommandContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                EphemeralChatCommandContext::class ->
                    EphemeralChatCommandContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as ChatInputCommandInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                ButtonContext::class ->
                    ButtonContext(
                        kord,
                        kfox,
                        translationModule,
                        event as ButtonInteractionCreateEvent,
                        source,
                        registry
                    )

                PublicButtonContext::class ->
                    PublicButtonContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as ButtonInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                EphemeralButtonContext::class ->
                    EphemeralButtonContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as ButtonInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                SelectMenuContext::class ->
                    SelectMenuContext(
                        kord,
                        kfox,
                        translationModule,
                        event as SelectMenuInteractionCreateEvent,
                        source,
                        registry
                    )

                PublicSelectMenuContext::class ->
                    PublicSelectMenuContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                EphemeralSelectMenuContext::class ->
                    EphemeralSelectMenuContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as SelectMenuInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                ModalContext::class ->
                    ModalContext(
                        kord,
                        kfox,
                        translationModule,
                        event as ModalSubmitInteractionCreateEvent,
                        source,
                        null,
                        registry
                    )

                PublicModalContext::class ->
                    PublicModalContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as ModalSubmitInteractionCreateEvent).interaction.deferPublicResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                EphemeralModalContext::class ->
                    EphemeralModalContext(
                        kord,
                        kfox,
                        translationModule,
                        (event as ModalSubmitInteractionCreateEvent).interaction.deferEphemeralResponseUnsafe(),
                        event,
                        source,
                        registry
                    )

                else -> throw IllegalArgumentException("Failed to wire parameter \"${parameter.name}\".")
            }
            context
        }
        if (context != null)
            for (filter in filters)
                if (!filter.doFilter(context!!))
                    return
        try {
            callSuspendBy(
                parameters
            )
        } catch (_: KFoxException) {
        } catch (e: Exception) {
            if (context == null) {
                logger.catching(e)
                return
            }

            val c = "Something went wrong, try again later!" // TODO: Localize
            when (context) {
                is PublicChatCommandContext -> (context as PublicChatCommandContext<T>).response.createPublicFollowup {
                    content = c
                }

                is PublicModalContext -> (context as PublicModalContext<T>).response.createPublicFollowup {
                    content = c
                }

                is PublicButtonContext -> (context as PublicButtonContext<T>).response.createPublicFollowup {
                    content = c
                }

                is PublicSelectMenuContext -> (context as PublicSelectMenuContext<T>).response.createPublicFollowup {
                    content = c
                }

                is EphemeralChatCommandContext -> (context as EphemeralChatCommandContext<T>).response.createEphemeralFollowup {
                    content = c
                }

                is EphemeralModalContext -> (context as EphemeralModalContext<T>).response.createEphemeralFollowup {
                    content = c
                }

                is EphemeralButtonContext -> (context as EphemeralButtonContext<T>).response.createEphemeralFollowup {
                    content = c
                }

                is EphemeralSelectMenuContext -> (context as EphemeralSelectMenuContext<T>).response.createEphemeralFollowup {
                    content = c
                }

                is ChatCommandContext -> (context as ChatCommandContext<T>).event.interaction.respondEphemeral {
                    content = c
                }

                is ButtonContext -> (context as ButtonContext<T>).event.interaction.respondEphemeral {
                    content = c
                }

                is SelectMenuContext -> (context as SelectMenuContext<T>).event.interaction.respondEphemeral {
                    content = c
                }

                is ModalContext -> (context as ModalContext<T>).event.interaction.respondEphemeral {
                    content = c
                }

                null -> {}
            }
            logger.catching(e)
        }
    }
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

fun scanForCommands(translationProvider: TranslationProvider, reflections: Reflections): List<CommandData> {
    val localCommands = reflections.getMethodsAnnotatedWith(Command::class.java)
        .map { it.kotlinFunction!! }
        .map { function ->
            val annotation = function.findAnnotation<Command>()!!
            val module = function.findAnnotation<Module>()?.module ?: translationProvider.defaultModule
            val subCommand = function.findAnnotation<SubCommand>()
            val group = function.findAnnotation<Group>()
            val filters = function.findAnnotation<Filter>()

            val permissionAnnotations = function.annotations
                .mapNotNull { it.annotationClass.findAnnotation<DefaultPermission>() }

            val defaultPermission = if (permissionAnnotations.isEmpty()) {
                null
            } else {
                Permissions(
                    permissionAnnotations.map {
                        Permission.Unknown(it.permission)
                    }
                )
            }

            CommandData(
                translationProvider.getString(
                    annotation.nameKey,
                    locale = translationProvider.defaultLocale,
                    module = module
                ),
                translationProvider.getString(
                    annotation.descriptionKey,
                    locale = translationProvider.defaultLocale,
                    module = module
                ),
                annotation.nameKey,
                annotation.descriptionKey,
                module,
                function,
                function.parameters.map {
                    val p = it.findAnnotation<Parameter>()
                    val pModule = it.findAnnotation<Module>()?.module ?: module
                    val choices = it.findAnnotation<Choices>()

                    ParameterData(
                        if (p == null) null else translationProvider.getString(
                            p.nameKey,
                            locale = translationProvider.defaultLocale,
                            module = pModule
                        ),
                        if (p == null) null else translationProvider.getString(
                            p.descriptionKey,
                            locale = translationProvider.defaultLocale,
                            module = pModule
                        ),
                        p?.nameKey,
                        p?.descriptionKey,
                        pModule,
                        choices?.list?.map { choice -> Choice.StringChoice(choice, Optional(null), choice) }
                            ?.toMutableList(),
                        it
                    )
                }.associateBy { it.parameter.name!! },
                filters?.filters?.mapNotNull { it.objectInstance as dev.bitflow.kfox.filter.Filter? }?.toSet()
                    ?: emptySet(),
                if (annotation.guild != Long.MIN_VALUE) Snowflake(annotation.guild) else null,
                if (group == null) null else GroupData(
                    translationProvider.getString(
                        group.nameKey,
                        locale = translationProvider.defaultLocale,
                        module = module
                    ),
                    translationProvider.getString(
                        group.descriptionKey,
                        locale = translationProvider.defaultLocale,
                        module = module
                    ),
                    group.nameKey,
                    group.descriptionKey
                ),
                if (subCommand == null) null else translationProvider.getString(
                    translationProvider.getString(
                        subCommand.parentNameKey,
                        locale = translationProvider.defaultLocale,
                        module = module
                    ),
                    locale = translationProvider.defaultLocale,
                    module = module
                ),
                emptyList(),
                defaultPermission
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
    fun getLocalization(key: String, module: String): MutableMap<Locale, String> =
        translationProvider.getAllStrings(key, module = module).toMutableMap()

    fun SubCommandBuilder.addSubCommand(command: CommandData) {
        nameLocalizations = getLocalization(command.nameKey, command.translationModule)
        descriptionLocalizations = getLocalization(command.descriptionKey, command.translationModule)
        addParameters(translationProvider, command)
    }

    for (command in localCommands.filter { it.parent == null }) {
        val children = localCommands.filter { it.parent == command.defaultName }
        input(command.defaultName, command.defaultDescription) {
            nameLocalizations = getLocalization(command.nameKey, command.translationModule)
            descriptionLocalizations = getLocalization(command.descriptionKey, command.translationModule)

            for (child in children) {
                if (child.group != null)
                    group(child.group.defaultName, child.group.defaultDescription) {
                        nameLocalizations = getLocalization(child.group.nameKey, command.translationModule)
                        descriptionLocalizations =
                            getLocalization(child.group.descriptionKey, command.translationModule)
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

            defaultMemberPermissions = command.permissions
        }
    }
}

private fun BaseInputChatBuilder.addParameters(translationProvider: TranslationProvider, command: CommandData) {
    fun OptionsBuilder.localize(parameter: ParameterData) {
        nameLocalizations = translationProvider.getAllStrings(
            parameter.nameKey!!,
            module = parameter.translationModule!!
        ).toMutableMap()
        descriptionLocalizations = translationProvider.getAllStrings(
            parameter.descriptionKey!!,
            module = parameter.translationModule
        ).toMutableMap()
    }

    for (parameter in command.parameters) {
        val name = parameter.value.defaultName
        val description = parameter.value.defaultDescription
        if (name == null || description == null)
            continue

        val nullable = parameter.value.parameter.type.isMarkedNullable
        when (parameter.value.parameter.type.classifier) {
            Attachment::class -> attachment(name, description) { required = !nullable }

            Boolean::class -> boolean(name, description) {
                localize(parameter.value)

                required = !nullable
            }

            Double::class -> number(name, description) {
                localize(parameter.value)
                required = !nullable

                val options = parameter.value.parameter.findAnnotations<DoubleOptions>().firstOrNull()

                if (options != null) {
                    if (options.max < Double.MAX_VALUE) {
                        maxValue = options.max
                    }

                    if (options.min > Double.MIN_VALUE) {
                        minValue = options.min
                    }
                }
            }

            KordChannel::class -> channel(
                name,
                description
            ) {
                localize(parameter.value)

                required = !nullable

                val options = parameter.value.parameter.findAnnotations<ChannelType>()

                if (options.isNotEmpty()) {
                    channelTypes = options.map { KordChannelType.Unknown(it.channelType) }
                }
            }

            Long::class -> int(name, description) {
                localize(parameter.value)

                required = !nullable

                val options = parameter.value.parameter.findAnnotations<LongOptions>().firstOrNull()

                if (options != null) {
                    if (options.max < Double.MAX_VALUE) {
                        maxValue = options.max
                    }

                    if (options.min > Double.MIN_VALUE) {
                        minValue = options.min
                    }
                }
            }

            Role::class -> role(name, description) {
                localize(parameter.value)

                required = !nullable
            }

            String::class -> string(name, description) {
                localize(parameter.value)

                required = !nullable
                choices = parameter.value.choices

                val options = parameter.value.parameter.findAnnotations<StringOptions>().firstOrNull()

                if (options != null) {
                    if (options.maxLength < Int.MAX_VALUE) {
                        maxLength = options.maxLength
                    }

                    if (options.minLength > Int.MIN_VALUE) {
                        minLength = options.minLength
                    }
                }
            }

            User::class -> user(name, description) {
                localize(parameter.value)

                required = !nullable
            }

            else -> throw UnsupportedOperationException("Parameter of type ${parameter.value.parameter.type} is not supported.")
        }
    }
}

suspend inline fun Kord.KFox(
    `package`: String,
    translationProvider: TranslationProvider,
    componentRegistry: ComponentRegistry = MemoryComponentRegistry(),
    registerCommands: Boolean = true
): KFox<Event, AsKordEvent<Event>> {
    val e = events
    val kfox = KFox<Event, AsKordEvent<Event>> {
        this.`package` = `package`
        this.events = { e }
        this.mapper = { it }
        this.translationProvider = translationProvider
        this.componentRegistry = componentRegistry
    }

    if (registerCommands)
        kfox.putCommands(this)

    return kfox
}

fun <T, E : AsKordEvent<T>> KFox(init: KFoxBuilder<T, E>.() -> Unit): KFox<T, E> =
    KFoxBuilder<T, E>().apply(init).build()

class KFoxBuilder<T, E : AsKordEvent<T>> internal constructor() {
    lateinit var `package`: String

    lateinit var events: (KFox<T, E>) -> SharedFlow<T>

    lateinit var mapper: E

    var translationProvider: TranslationProvider = ResourceBundleTranslationProvider()

    var componentRegistry: ComponentRegistry = MemoryComponentRegistry()

    fun build() = KFox(`package`, translationProvider, componentRegistry, events, mapper)
}

typealias AsKordEvent<T> = suspend ((T) -> Event?)
