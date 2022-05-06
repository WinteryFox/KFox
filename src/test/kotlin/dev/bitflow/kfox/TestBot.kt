package dev.bitflow.kfox

import dev.bitflow.kfox.contexts.*
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

const val BUTTON_CALLBACK = "button"
const val MENU_CALLBACK = "menu"
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

@Button(BUTTON_CALLBACK)
suspend fun testButton(
    context: PublicButtonContext
) {
    with(context) {
        response.createPublicFollowup {
            content = "Awk!"
        }
    }
}

@SelectMenu(MENU_CALLBACK)
suspend fun testMenu(
    context: PublicSelectMenuContext
) {
    with(context) {
        response.createPublicFollowup {
            content = "You picked \"${event.interaction.values.first()}\"! Awk!"
        }
    }
}

@Command("test", "test")
fun test() {}

@Command("sub", "Test")
@SubCommand("test")
suspend fun testSub(
    context: PublicChatCommandContext,
    @Parameter("value", "content-key")
    value: String?
) {
    context.response.createPublicFollowup {
        content = "Your value was $value"
    }
}

@Command("parrot", "parrot-key")
suspend fun testCommand(
    context: ChatCommandContext,
    @Parameter("content", "content-key")
    value: String
) {
    with(context) {
        event.interaction.modal("Hello!", "aaa") {
            actionRow {
                textInput(TextInputStyle.Short, "poem", "Poetry night") {
                    placeholder = "Let out your inner Shakespeare"
                }
            }

            actionRow {
                textInput(TextInputStyle.Short, "name", "What's your name?") {
                    placeholder = "✨ Amy"
                }
            }

            register(MODAL_CALLBACK)
        }
    }
}

@Command("parakeet", "Something")
@SubCommand("birds")
@Group("parakeet", "This group contains birbs")
suspend fun subCommandWithCategory(
    context: PublicChatCommandContext
) {
    context.response.createPublicFollowup {
        content = "It's Friday my dudes"
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
    fun testBot() = runBlocking {
        client.listen("dev.bitflow.kfox") {
            createGuildApplicationCommands(Snowflake(809278232100077629)) {
                registerCommands(scanForCommands(reflections("dev.bitflow.kfox")))
            }
        }

        client.login()
    }
}
