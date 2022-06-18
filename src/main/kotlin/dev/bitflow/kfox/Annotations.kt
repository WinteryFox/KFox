package dev.bitflow.kfox

@Target(AnnotationTarget.FUNCTION)
annotation class Module(
    val module: String
)

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val nameKey: String,
    val descriptionKey: String,
    val guild: Long = Long.MIN_VALUE
)

@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val parentNameKey: String
)

annotation class Group(
    val nameKey: String,
    val descriptionKey: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val nameKey: String,
    val descriptionKey: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Choices(
    val list: Array<String>
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
