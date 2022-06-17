package dev.bitflow.kfox

import dev.bitflow.kfox.contexts.*
import dev.bitflow.kfox.localization.ResourceBundleTranslationProvider
import dev.kord.common.Locale
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

const val MODAL_CALLBACK = "modal"

@Command("name", "description", guild = 809278232100077629L)
@Module("help")
suspend fun help(
    context: EphemeralChatCommandContext
) = with(context) {
    response.createEphemeralFollowup {
        content = getUserString("content")
    }
}

@Modal(MODAL_CALLBACK)
suspend fun modal(
    context: PublicModalContext,
    @ModalValue("poem")
    modalValue: String,
    @ModalValue("name")
    what: String
) = with(context) {
    response.createPublicFollowup {
        content = "Your beautiful poem reads; $modalValue and your name is $what"
    }
}

@Command("root-name", "root-desc", guild = 809278232100077629L)
suspend fun root() {
}

@Command("sub-name", "sub-desc", guild = 809278232100077629L)
@SubCommand("root-name")
@Group("group-name", "group-desc")
suspend fun sub(context: ChatCommandContext) = with(context) {
    event.interaction.respondEphemeral { content = "This is a sub-command" }
}

@Command("parrot-name", "parrot-desc", guild = 809278232100077629L)
suspend fun testCommand(
    context: ChatCommandContext,
    @Parameter("content-name", "content-desc")
    value: String
) {
    with(context) {
        event.interaction.modal(getUserString("name"), "aaa") {
            actionRow {
                textInput(TextInputStyle.Short, "poem", getUserString("poetry")) {
                    placeholder = getUserString("poetry-placeholder")
                }
            }

            actionRow {
                textInput(TextInputStyle.Short, "name", getUserString("name")) {
                    placeholder = getUserString("name-placeholder")
                }
            }

            register(MODAL_CALLBACK)
        }
    }
}

class TestBot {
    private var client: Kord

    init {
        runBlocking {
            client = Kord(System.getenv("TOKEN"))
        }
    }

    @Test
    fun testBot(): Unit = runBlocking {
        val translationProvider =
            ResourceBundleTranslationProvider("test", Locale.ENGLISH_UNITED_STATES)
        val kfox = client.kfox("dev.bitflow.kfox", translationProvider)
        kfox.listen()
        client.login()
        return@runBlocking
    }
}
