package dev.kfox

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@Command("parrot", "parrot-key")
suspend fun testCommand(
    context: CommandContext,
    @Parameter("content", "content-key")
    value: String
) {
    context.client.rest.interaction.createFollowupMessage(
        context.event.interaction.applicationId,
        context.event.interaction.token
    ) {
        content = "Hi, I'm a friendly parakeet! You said \"$value,\" awk!"
    }
}

class TestBot {
    @OptIn(KordExperimental::class)
    private var client: Kord

    init {
        runBlocking {
            client = Kord(System.getenv("TOKEN"))
        }
    }

    @OptIn(KordExperimental::class)
    @Test
    fun testBot() = runBlocking {
        client.listen("dev.kfox") {
            it.getGuildApplicationCommands(Snowflake(809278232100077629)).toList()
        }

        client.login()
    }
}
