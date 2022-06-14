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

## Supported interactions

- [x] Commands
    - [x] Global
    - [x] Guild
    - [x] Parameters
    - [x] Sub-commands
    - [x] Groups
    - [ ] User commands
    - [ ] Message commands
    - [ ] Autocomplete
    - [ ] Localization
- [x] Buttons
- [x] Select menus
- [x] Modals

## Installation

TODO

## Examples

⚠️ KFox currently does not support automatically creating your commands on the Discord side of things.

With every type of interaction you will receive either a CommandContext or ComponentContext, these context
objects contain a variety of things like the Kord instance, the event and the response. For every type of
context there exists a Public and Ephemeral variant, depending on which one you request in your callback functions
the interaction will be handled as ephemeral, meaning that only the user who triggered the interaction can see
the reply, or public. All the examples below use the Public variant, you are free to change these to Ephemeral
as you please of course.

### Setup and commands

Setting up KFox is as easy as calling a single extension function on your Kord instance.

```kotlin
suspend fun main() {
    val kord = Kord("your_token_here")
    kord.listen() // 1
    kord.login()
}
```

1. Ensure you call `Kord#listen` ***before*** you call `Kord#login`.

#### Creating a command

```kotlin
@Command("parrot-desc", "I'm a friendly parakeet, I'll repeat what you said awk!") // 1
suspend fun testCommand(
    context: PublicChatCommandContext, // 2
    @Parameter("content-desc", "content-key") // 3
    value: String
) = with(context) {
    response.createPublicFollowup {
        content = "Hi, I'm a friendly parakeet! You said \"$value,\" awk!"
    }
}
```

1. Define a new function and annotate it with `@Command`.
2. Hook in your context; ephemeral responses use `EphemeralChatCommandContext`,
   whereas public responses use `PublicChatCommandContext`.
3. Define your parameters and annotate them with `@Parameter`.

#### Creating a sub-command (with a group)

```kotlin
@Command("parakeet", "A happy lil description.") // 1
@SubCommand("birds", "secret") // 2 & 3
suspend fun subCommandWithCategory(
    context: PublicChatCommandContext
) = with(context) {
    response.createPublicFollowup {
        content = "It looks like you found my cozy hideout, awk!"
    }
}
```

1. Annotate your callback function with `@Command`, the name will be the name of the sub-command.
2. Annotate it with `@SubCommand`, set the parent to the command we just created "parrot".
3. Simply fill in the group with something to add this sub-command to a group, leave blank otherwise.

### Components

All components need to be registered with the ComponentRegistry, the default implementation is an in-memory register,
this means that your components do not persist between restarts. Should you need such functionality, implement the
interface and store the IDs persistently. You can then pass an instance of your custom implementation in
your `Kord#listen` call.

#### Buttons

```kotlin
const val CALLBACK_ID = "unique_callback_id"

@Button(CALLBACK_ID) // 1
suspend fun testButton(
    context: PublicButtonContext
) = with(context) {
    response.createPublicFollowup {
        content = "Awk!"
    }
}

@Command("parakeetbutton", "Press the button, awk?") // 2
suspend fun testCommand(
    context: PublicChatCommandContext,
) = with(context) {
    response.createPublicFollowup {
        actionRow {
            interactionButton(ButtonStyle.Primary, "my_custom_button_id") {
                label = "Awk?"

                register(CALLBACK_ID) // 3
            }
        }
    }
}
```

1. Define a new function and annotate it with `@Button`, make sure the ID you specify is unique as this will be
   used internally to find and call this callback.
2. Note that you do not necessarily need to create a new command to use buttons, but we do here for convenience.
3. You **must** call register within components, this will inform KFox that this is a component we need to track
   and will create the link between this button and the callback function.

#### Select menus

```kotlin
const val MENU_CALLBACK = "unique_callback_id_2"

@SelectMenu(MENU_CALLBACK) // 1
suspend fun testMenu(
    context: PublicSelectMenuContext
) = with(context) {
    response.createPublicFollowup {
        content = "You picked \"${event.interaction.values.first()}\"! Awk!"
    }
}

@Command("parrotpick", "Pick whichever suits you most, awk!")
suspend fun testCommand(
    context: PublicChatCommandContext
) = with(context) {
    response.createPublicFollowup {
        actionRow {
            selectMenu("your_menu_id") {
                placeholder = "Select your favourite animal!"
                option("Foxes", "foxes")
                option("Bunnies", "bunnies")
                option("Cats", "cats")
                option("Dogs", "dogs")

                register(MENU_CALLBACK) // 2
            }
        }
    }
}
```

1. Define a new function and annotate it with `@Button`, make sure the ID you specify is unique as this will be
   used internally to find and call this callback.
2. You **must** call register within components, this will inform KFox that this is a component we need to track
   and will create the link between this button and the callback function.
