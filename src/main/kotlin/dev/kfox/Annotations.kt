package dev.kfox

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String,
    val description: String, // TODO: Localize these two
)

@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val parent: String,
    val group: String = ""
)

@Target(AnnotationTarget.FUNCTION)
annotation class Button(
    val callbackId: String
)

@Target(AnnotationTarget.FUNCTION)
annotation class SelectMenu(
    val callbackId: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val name: String, // TODO: Localize these two
    val description: String
)
