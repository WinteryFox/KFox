package dev.bitflow.kfox.annotations.processor.permissions

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import dev.kord.common.entity.Permission

private const val ANNOTATION = "dev.bitflow.kfox.annotations.GenerateForPermissions"

/**
 * Annotation processor that exists to generate annotations representing the various default permissions a command
 * can have, allowing them to be set easily via a single annotation in your code.
 *
 * You really only ever want this to be run once per project, but there's nothing stopping you from using the
 * GenerateForPermissions annotation, if you want that for some reason.
 *
 * The annotated annotation class must take exactly one parameter - a [Long] representing the permission bitfield
 * number. It could probably be an [Integer], but `DiscordBitSet` uses [Long]s, so we're using them here too.
 */
class PermissionsProcessor(
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

        // Find all annotated class declarations that validate, and pass them through the `PermissionsVisitor`, which
        // will process them individually.
        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(PermissionsVisitor(), Unit) }

        return ret
    }

    inner class PermissionsVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val permissions = Permission.values

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

                    permissions
                        .sortedBy { it::class.simpleName }  // Makes logging more readable
                        .forEach {
                            logger.info("${it::class.simpleName} -> ${it.code.value}")

                            appendLine("// Permission: ${it::class.simpleName}")
                            appendLine("@${classDeclaration.simpleName.asString()}(${it.code.value})")

                            appendLine("@Target(AnnotationTarget.FUNCTION)")
                            appendLine("annotation class DefaultPermission${it::class.simpleName}")

                            // Two extra blank lines, as is convention
                            appendLine("")
                            appendLine("")
                        }
                }.encodeToByteArray()
            )
        }
    }
}
