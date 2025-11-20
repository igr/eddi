package dev.oblac.eddi.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.oblac.eddi.ksp.EventProcessor.Companion.TAG_INTERFACE_NAME
import java.util.*

internal fun capitalize(str: String) = str.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

internal fun tagPropertiesOfRecord(eventClass: KSClassDeclaration): List<String> =
    (eventClass.primaryConstructor?.parameters
        ?.filter { param ->
            val paramType = param.type.resolve()
            val declaration = paramType.declaration
            declaration.qualifiedName?.asString() == TAG_INTERFACE_NAME ||
                    (declaration as? KSClassDeclaration)?.superTypes?.any { superType ->
                        superType.resolve().declaration.qualifiedName?.asString() == TAG_INTERFACE_NAME
                    } == true
        }
        ?.mapNotNull { it.name?.asString() }
        ?: emptyList())