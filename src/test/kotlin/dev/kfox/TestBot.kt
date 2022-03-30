package dev.kfox

import dev.kfox.contexts.PublicButtonContext
import dev.kfox.contexts.PublicChatCommandContext
import dev.kfox.contexts.PublicSelectMenuContext
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.rest.builder.message.create.actionRow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

const val BUTTON_CALLBACK = "button"
const val MENU_CALLBACK = "menu"

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

@Command("parrot", "parrot-key")
suspend fun testCommand(
    context: PublicChatCommandContext,
    @Parameter("content", "content-key")
    value: String
) {
    with(context) {
        response.createPublicFollowup {
            content = "Hi, I'm a friendly parakeet! You said \"$value,\" awk!"
            actionRow {
                selectMenu("menuId") {
                    placeholder = "Select your favourite food!"
                    option("Foxes", "foxes")
                    option("Bunnies", "amys")
                    option("Cats", "cats")
                    option("Dogs", "dogs")

                    register(MENU_CALLBACK)
                }
            }
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
    fun testBot() = runBlocking {
        client.listen("dev.kfox") {
            it.getGuildApplicationCommands(Snowflake(809278232100077629))
        }

        client.login()
    }
}
