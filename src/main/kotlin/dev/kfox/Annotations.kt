package dev.kfox

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val name: String,
    val descriptionKey: String,
    //val category: Category,
    val applicationIds: LongArray = [],
    val ephemeral: Boolean = false,
)

@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val parent: String
)

@Target(AnnotationTarget.FUNCTION)
annotation class Button(
    val callbackId: String,
    val ephemeral: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
annotation class SelectMenu(
    val callbackId: String,
    val ephemeral: Boolean = false
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(
    val name: String, val descriptionKey: String
)
