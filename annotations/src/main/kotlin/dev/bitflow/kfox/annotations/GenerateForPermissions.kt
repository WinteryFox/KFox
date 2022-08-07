package dev.bitflow.kfox.annotations

/**
 * Generate annotations for default permissions based on Kord's built-in Permission type, applying the annotated
 * annotation to them.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class GenerateForPermissions
