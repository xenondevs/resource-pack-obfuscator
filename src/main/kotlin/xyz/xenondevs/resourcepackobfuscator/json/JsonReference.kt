package xyz.xenondevs.resourcepackobfuscator.json

import com.google.gson.JsonObject
import xyz.xenondevs.resourcepackobfuscator.util.GSON
import xyz.xenondevs.resourcepackobfuscator.util.type
import java.lang.reflect.Type
import kotlin.reflect.KProperty

internal inline fun <reified T> JsonObject.reference(key: String): JsonReference<T> =
    JsonReference(this, key, type<T>())

internal inline fun <reified T> JsonObject.referenceNullable(key: String): NullableJsonReference<T?> =
    NullableJsonReference(this, key, type<T?>())

internal class JsonReference<T>(
    private val jsonObject: JsonObject,
    private val key: String,
    private val type: Type
) {
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        val element = jsonObject.get(key)
        return GSON.fromJson(element, type) as T
    }
    
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        GSON.toJsonTree(value, type)
    }
    
}

internal class NullableJsonReference<T>(
    private val jsonObject: JsonObject,
    private val key: String,
    private val type: Type
) {
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val element = jsonObject.get(key) ?: return null
        return GSON.fromJson(element, type) as T
    }
    
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        GSON.toJsonTree(value, type)
    }
    
}