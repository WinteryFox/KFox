package dev.bitflow.kfox.annotations.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.bitflow.kfox.annotations.processor.permissions.PermissionsProcessor

class PermissionsProcessorProvider : SymbolProcessorProvider {
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
		PermissionsProcessor(environment.codeGenerator, environment.logger)
}
