package dev.bitflow.kfox.annotations.processor.channels

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import dev.kord.common.entity.ChannelType

private const val ANNOTATION = "dev.bitflow.kfox.annotations.GenerateForChannelTypes"

/**
 * Annotation processor that exists to generate annotations representing the various channel types a channel command
 * option can require, allowing them to be set easily via a single annotation in your code.
 *
 * You really only ever want this to be run once per project, but there's nothing stopping you from using the
 * GenerateForChannelTypes annotation twice, if you want that for some reason.
 *
 * The annotated annotation class must take exactly one parameter - an [Int] representing the channel type enum value.
 */
class ChannelTypesProcessor(
    private val generator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all annotated symbols
        val symbols = resolver.getSymbolsWithAnnotation(ANNOTATION)

        // Keep track of all symbols that KSP isn't able to validated (or resolve) right now
        val ret = symbols.filter { !it.validate() }.toList()

        ret.forEach {
            // Log each symbol that couldn't be validated
            logger.warn("Unable to validate: $it")
        }

        // Find all annotated class declarations that validate, and pass them through the `ChannelTypesVisitor`, which
        // will process them individually.
        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(ChannelTypesVisitor(), Unit) }

        return ret
    }

    inner class ChannelTypesVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val channelTypes = ChannelType::class.sealedSubclasses  // Get all sealed subtypes
                .mapNotNull { it.objectInstance }  // Filter and map to only those that are `object`s
                .filter { it::class.annotations.filterIsInstance<Deprecated>().isEmpty() }  // Filter out deprecations

            // Create a new file that matches the file the class was defined within, appending `Annotations` to the
            // filename before the `.kt` extension, which KSP fills in automatically.
            val file = generator.createNewFile(
                Dependencies(true, classDeclaration.containingFile!!),
                classDeclaration.packageName.asString(),
                classDeclaration.simpleName.asString() + "Annotations"
            )

            file.write(
                buildString {
                    // Generate the file header first, including the package declaration
                    appendLine(
                        """
                            package ${classDeclaration.packageName.asString()}

                            // Original annotation class, for safety
                            import ${classDeclaration.qualifiedName!!.asString()}

						""".trimIndent()
                    )

                    channelTypes
                        .sortedBy { it::class.simpleName }  // Makes logging more readable
                        .forEach {
                            logger.info("${it::class.simpleName} -> ${it.value}")

                            appendLine("// Channel Type: ${it::class.simpleName}")
                            appendLine("@${classDeclaration.simpleName.asString()}(${it.value})")

                            appendLine("@Target(AnnotationTarget.VALUE_PARAMETER)")
                            appendLine("annotation class ChannelType${it::class.simpleName}")

                            // Two extra blank lines, as is convention
                            appendLine("")
                            appendLine("")
                        }
                }.encodeToByteArray()
            )
        }
    }
}
