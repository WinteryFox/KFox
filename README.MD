<a href=""><img align="right" src="https://zerotwo.bot/logo.webp" width=27%></a>

[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/ZeroTwo-Bot/KFox/CI/master?logo=github&&style=for-the-badge)](https://github.com/ZeroTwo-Bot/KFox/actions)
[![Discord](https://img.shields.io/static/v1?label=Discord&message=Click%20here&color=7289DA&style=for-the-badge&logo=discord)](https://zerotwo.link/support)

# KFox

While using Kord, have you ever stopped and thought to yourself; "I wonder how I could add a command
that adds a button that shows a fox whenever I want." Well dear user, I have!
To solve this problem I, Amy (ya queen, ya girl), decided to create a library that will let you
create commands, components and other Discord interactions easily, quickly and effortlessly.

#### Meme over, serious only now

KFox is a Discord interaction library written in Kotlin built on-top of [Kord](https://kord.dev/).
It will allow you to easily create and respond to any type of interaction that Kord (and by extension Discord) supports.

## Installation

TODO

## Examples

⚠️ KFox currently does not support automatically creating your commands on the Discord side of things.

### Basic setup and first command

```kotlin
@Command("parrot", "Awk! I'll repeat whatever you say, awk!") // 2
suspend fun testCommand(
    context: PublicChatCommandContext, // 3
    @Parameter("content", "content-key") // 4
    value: String
) {
    context.response.createPublicFollowup {
        content = "Hi, I'm a friendly parakeet! You said \"$value,\" awk!"
    }
}

suspend fun main() {
    val kord = Kord("your_token_here")
    kord.listen() // 1
    kord.login()
}
```
1. Ensure you call `Kord#listen` ***before*** you call `Kord#login`.
2. Define a new function and annotate it with `@Command`.
3. Hook in your context; ephemeral responses use `EphemeralChatCommandContext`,
whereas public responses use `PublicChatCommandContext`.
4. Define your parameters and annotate them with `@Parameter`.
