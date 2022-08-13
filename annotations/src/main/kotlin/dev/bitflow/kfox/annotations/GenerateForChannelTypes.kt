package dev.bitflow.kfox.annotations

/**
 * Generate annotations for required channel types based on Kord's built-in ChannelType type, applying the annotated
 * annotation to them.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class GenerateForChannelTypes
