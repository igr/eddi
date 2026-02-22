package dev.oblac.eddi.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.oblac.eddi.ksp.EventProcessor.Companion.TAG_INTERFACE_NAME
import java.util.*

internal fun capitalize(str: String) = str.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

internal fun simpleClassNameOf(qualifiedName: String): String {
    val lastDotIndex = qualifiedName.lastIndexOf('.')
    return if (lastDotIndex != -1 && lastDotIndex < qualifiedName.length - 1) {
        qualifiedName.substring(lastDotIndex + 1)
    } else {
        qualifiedName
    }
}

internal data class TagProperty(val name: String, val tagType: String, val nullable: Boolean)

/**
 * Returns tag properties for constructor parameters that implement [Tag].
 */
internal fun tagPropertiesOfRecord(eventClass: KSClassDeclaration): List<TagProperty> =
    (eventClass.primaryConstructor?.parameters
        ?.mapNotNull { param ->
            val paramType = param.type.resolve()
            val declaration = paramType.declaration
            val isTag = declaration.qualifiedName?.asString() == TAG_INTERFACE_NAME ||
                    (declaration as? KSClassDeclaration)?.superTypes?.any { superType ->
                        superType.resolve().declaration.qualifiedName?.asString() == TAG_INTERFACE_NAME
                    } == true
            if (isTag) {
                val name = param.name?.asString() ?: return@mapNotNull null
                val tagType = declaration.qualifiedName?.asString() ?: return@mapNotNull null
                TagProperty(name, tagType, paramType.isMarkedNullable)
            } else null
        }
        ?: emptyList())