package dev.oblac.eddi.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Provider for [EventProcessor].
 */
class EventProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return EventProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            env = environment
        )
    }
}
