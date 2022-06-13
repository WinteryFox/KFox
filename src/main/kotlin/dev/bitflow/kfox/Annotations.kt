package dev.bitflow.kfox

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String,
    val descriptionKey: String,
    val guild: Long = Long.MIN_VALUE
)

@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val parent: String
)

annotation class Group(
    val name: String,
    val description: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val name: String,
    val descriptionKey: String
)

@Target(AnnotationTarget.FUNCTION)
annotation class Button(
    val callbackId: String
)

@Target(AnnotationTarget.FUNCTION)
annotation class SelectMenu(
    val callbackId: String
)

@Target(AnnotationTarget.FUNCTION)
annotation class Modal(
    val callbackId: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ModalValue(
    val customId: String
)
