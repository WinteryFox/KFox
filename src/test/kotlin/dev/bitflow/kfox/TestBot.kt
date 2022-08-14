package dev.bitflow.kfox

import dev.bitflow.kfox.context.*
import dev.bitflow.kfox.filter.GuildFilter
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
@Filter(GuildFilter::class)
suspend fun help(
    context: EphemeralChatCommandContext<*>
) = with(context) {
    response.createEphemeralFollowup {
        content = getUserString("content")
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
            ResourceBundleTranslationProvider(defaultModule = "test", defaultLocale = Locale.ENGLISH_UNITED_STATES)
        val kfox = client.KFox("dev.bitflow.kfox", translationProvider)
        kfox.listen()
        client.login()
        return@runBlocking
    }
}
