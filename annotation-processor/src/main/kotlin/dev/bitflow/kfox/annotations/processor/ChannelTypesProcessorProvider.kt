package dev.bitflow.kfox.annotations.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.bitflow.kfox.annotations.processor.channels.ChannelTypesProcessor

class ChannelTypesProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ChannelTypesProcessor(environment.codeGenerator, environment.logger)
}
