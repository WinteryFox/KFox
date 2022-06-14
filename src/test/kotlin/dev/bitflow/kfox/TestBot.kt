package dev.bitflow.kfox

import dev.bitflow.kfox.contexts.*
import dev.kord.common.Locale
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.test.Test

const val MODAL_CALLBACK = "modal"

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

@Command("root-name", "root-desc", 809278232100077629L)
suspend fun root() {
}

@Command("sub-name", "sub-desc", 809278232100077629L)
@SubCommand("root-name")
@Group("group-name", "group-desc")
suspend fun sub(context: ChatCommandContext) = with(context) {
    event.interaction.respondEphemeral { content = "This is a sub-command" }
}

@Command("parrot-name", "parrot-desc", 809278232100077629L)
suspend fun testCommand(
    context: ChatCommandContext,
    @Parameter("content-name", "content-desc")
    value: String
) {
    with(context) {
        event.interaction.modal(getUserString("title"), "aaa") {
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
        val bundleJa = ResourceBundle.getBundle("commands", java.util.Locale("ja", "JP"))
        val bundleEn = ResourceBundle.getBundle("commands", java.util.Locale("en", "US"))
        val kfox = client.kfox(
            "dev.bitflow.kfox",
            mapOf(
                Pair(Locale("ja"), bundleJa),
                Pair(Locale("en", "US"), bundleEn)
            )
        )
        kfox.listen()
        client.login()
        return@runBlocking
    }
}
