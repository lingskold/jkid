package ru.yole.jkid.serialization

import ru.yole.jkid.*
import java.text.SimpleDateFormat
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters

fun serialize(obj: Any): String = buildString { serializeObject(obj) }

/* the first implementation discussed in the book */
private fun StringBuilder.serializeObjectWithoutAnnotation(obj: Any) {
    val kClass = obj.javaClass.kotlin
    val properties = kClass.memberProperties

    properties.joinToStringBuilder(this, prefix = "{", postfix = "}") { prop ->
        serializeString(prop.name)
        append(": ")
        serializePropertyValue(prop.get(obj))
    }
}

private fun StringBuilder.serializeObject(obj: Any) {
    obj.javaClass.kotlin.memberProperties
            .filter { it.findAnnotation<JsonExclude>() == null }
            .joinToStringBuilder(this, prefix = "{", postfix = "}") {
                serializeProperty(it, obj)
            }
}

private fun StringBuilder.serializeProperty(
        prop: KProperty1<Any, *>, obj: Any
) {
    val jsonNameAnn = prop.findAnnotation<JsonName>()
    val propName = jsonNameAnn?.name ?: prop.name
    serializeString(propName)
    append(": ")

    val value = prop.get(obj)

    serializePropertyValue(prop.getSerializer()?.toJsonValue(value) ?: value)
}

fun KProperty<*>.getDateFormatString(): String? {
    val dateFormatAnnotation = findAnnotation<DateFormat>()
    return dateFormatAnnotation?.format
}

fun KProperty<*>.getSerializer(): ValueSerializer<Any?>? {
    val customSerializerAnn = findAnnotation<CustomSerializer>()
    val serializerClass = customSerializerAnn?.serializerClass

    val dateSerializer = if (getDateFormatString() != null) DateSerializer(getDateFormatString()) else null

    val valueSerializer = serializerClass?.objectInstance
            ?: serializerClass?.createInstance()
            ?: dateSerializer

    @Suppress("UNCHECKED_CAST")

    return valueSerializer as ValueSerializer<Any?>
}


private fun StringBuilder.serializePropertyValue(value: Any?) {
    when (value) {
        null -> append("null")
        is String -> serializeString(value)
        is Number, is Boolean -> append(value.toString())
        is List<*> -> serializeList(value)
        else -> serializeObject(value)
    }
}

private fun StringBuilder.serializeList(data: List<Any?>) {
    data.joinToStringBuilder(this, prefix = "[", postfix = "]") {
        serializePropertyValue(it)
    }
}

private fun StringBuilder.serializeString(s: String) {
    append('\"')
    s.forEach { append(it.escape()) }
    append('\"')
}

private fun Char.escape(): Any =
        when (this) {
            '\\' -> "\\\\"
            '\"' -> "\\\""
            '\b' -> "\\b"
            '\u000C' -> "\\f"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            else -> this
        }
