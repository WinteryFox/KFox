package dev.bitflow.kfox

import dev.bitflow.kfox.annotations.GenerateForChannelTypes
import dev.bitflow.kfox.annotations.GenerateForPermissions
import kotlin.reflect.KClass

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

@Target(AnnotationTarget.FUNCTION)
annotation class Filter(
    vararg val filters: KClass<*>
)

@GenerateForPermissions
@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class DefaultPermission(
    val permission: Long
)

@GenerateForChannelTypes
@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class ChannelType(
    val channelType: Int
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class LongOptions(
    val min: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DoubleOptions(
    val min: Double = Double.MIN_VALUE,
    val max: Double = Double.MAX_VALUE
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class StringOptions(
    val minLength: Int = Int.MIN_VALUE,
    val maxLength: Int = Int.MAX_VALUE
)
