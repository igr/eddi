package dev.oblac.eddi.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import java.io.BufferedWriter

/**
 * KSP processor that generates Event classes.
 */
class EventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val env: SymbolProcessorEnvironment
) : SymbolProcessor {

    companion object Companion {
        internal const val EVENT_INTERFACE_NAME = "dev.oblac.eddi.Event"
        internal const val TAG_INTERFACE_NAME = "dev.oblac.eddi.Tag"
    }

    private val processedEventClasses = mutableSetOf<String>()
    private val eventClasses = mutableSetOf<KSClassDeclaration>()
    private val tagImplementations = mutableMapOf<String, String>() // Map of TagClass -> EventType

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val eventInterface = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString(EVENT_INTERFACE_NAME)
        )

        if (eventInterface == null) {
            logger.warn("Event interface not found: $EVENT_INTERFACE_NAME")
            return emptyList()
        }

        val eventImplementations = resolver.getNewFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { classDeclaration ->
                classDeclaration.superTypes.any { superType ->
                    val resolvedType = superType.resolve()
                    val declaration = resolvedType.declaration
                    declaration.qualifiedName?.asString() == EVENT_INTERFACE_NAME
                }
            }
            .filter { it.validate() }
            .toList()

        eventImplementations.forEach { tagClass ->
            logger.info("Processing event class: ${tagClass.qualifiedName?.asString()}")
            val qualifiedName = tagClass.qualifiedName?.asString() ?: return@forEach
            if (qualifiedName !in processedEventClasses) {
                processedEventClasses.add(qualifiedName)
                generateEventCode(tagClass)
            }
        }

        // Detect all Tag implementations and collect class names with Tag generic
        val tagInterface = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString(TAG_INTERFACE_NAME)
        )

        if (tagInterface != null) {
            val tagImplementationsList = resolver.getNewFiles()
                .flatMap { it.declarations }
                .filterIsInstance<KSClassDeclaration>()
                .filter { classDeclaration ->
                    classDeclaration.superTypes.any { superType ->
                        val resolvedType = superType.resolve()
                        val declaration = resolvedType.declaration
                        declaration.qualifiedName?.asString() == TAG_INTERFACE_NAME
                    }
                }
                .filter { it.validate() }
                .toList()

            tagImplementationsList.forEach { tagClass ->
                val tagClassName = tagClass.qualifiedName?.asString()

                // Extract the generic type from Tag<EventType>
                val genericEventType = tagClass.superTypes
                    .firstOrNull { superType ->
                        val resolvedType = superType.resolve()
                        resolvedType.declaration.qualifiedName?.asString() == TAG_INTERFACE_NAME
                    }
                    ?.resolve()
                    ?.arguments
                    ?.firstOrNull()
                    ?.type
                    ?.resolve()
                    ?.declaration
                    ?.qualifiedName
                    ?.asString()

                if (tagClassName != null && genericEventType != null) {
                    tagImplementations[tagClassName] = genericEventType
                    logger.info("Detected Tag implementation: $tagClassName with generic type $genericEventType")
                }
            }
        }

        return emptyList()
    }

    override fun finish() {
        eventClasses.forEach { generateEventCode(it) }
        generateEventRegistry(processedEventClasses)
    }

    private fun generateEventCode(eventClass: KSClassDeclaration) {
        val packageName = eventClass.packageName.asString()
        val className = eventClass.simpleName.asString()
        val targetClassName = "${className}Event"

        val tagPropertiesOfEvent = tagPropertiesOfRecord(eventClass)

        val containingFile = eventClass.containingFile
        val dependencies = if (containingFile != null) {
            Dependencies(true, containingFile)
        } else {
            Dependencies(true)
        }

        val file = codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = packageName,
            fileName = targetClassName
        )

        file.bufferedWriter().use { writer ->
            writeSourceFile(writer, packageName, className, targetClassName, tagPropertiesOfEvent)
        }

        logger.info("Generated $packageName.$targetClassName for $className")
    }

    private fun writeSourceFile(
        writer: BufferedWriter,
        packageName: String,
        className: String,
        targetClassName: String,
        tagPropertiesOfEvent: List<String>
    ) {

        val refCtors = tagPropertiesOfEvent.joinToString(", ") {
            "dev.oblac.eddi.Events.refOf(event.$it)"
        }

        writer.write(
            """
                |package $packageName
                |
                |import dev.oblac.eddi.EventName
                |
                |/**
                | * Generated companion class for [$className].
                | * This is a marker class generated by TagFooProcessor.
                | */
                |object $targetClassName: dev.oblac.eddi.EventMeta<$className> {
                |    
                |    override val CLASS = $className::class
                |    override val NAME = EventName.of(CLASS) 
                |    
                |    override fun refs(event: $className): Array<dev.oblac.eddi.Ref> =
                |        arrayOf($refCtors)
                |}
            """.trimMargin()
        )
    }

    private fun generateEventRegistry(processedEventClasses: Set<String>) {
        val file = env.codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName = "dev.oblac.eddi.meta",
            fileName = "EventsRegistry"
        )

        val tagsRegister = "\n" + tagImplementations.map { (tagClass, eventType) ->
            "dev.oblac.eddi.Events.register($tagClass::class, ${eventType}Event.NAME)"
        }.joinToString("\n")

        file.write(
            """
            package dev.oblac.eddi.meta

            object EventsRegistry { 
                fun init() {
                    // Register EventMeta implementations
                    dev.oblac.eddi.Events.register(
                        listOf(${processedEventClasses.map { it + "Event" }.joinToString { it }})
                    )
                    // Register Tag implementations
                    $tagsRegister
                }
            }
            """.trimIndent().toByteArray()
        )
    }

}
