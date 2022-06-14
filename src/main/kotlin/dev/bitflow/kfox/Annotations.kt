package dev.bitflow.kfox

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val nameKey: String,
    val descriptionKey: String,
    val translationModule: String = "",
    val guild: Long = Long.MIN_VALUE
)

@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val parentNameKey: String,
    val translationModule: String = ""
)

annotation class Group(
    val nameKey: String,
    val descriptionKey: String,
    val translationModule: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val nameKey: String,
    val descriptionKey: String,
    val translationModule: String = ""
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
