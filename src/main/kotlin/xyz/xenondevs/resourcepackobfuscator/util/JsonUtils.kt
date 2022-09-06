package xyz.xenondevs.resourcepackobfuscator.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.lang.reflect.Type

internal val GSON = GsonBuilder().create()

internal fun File.parseJson(): JsonElement = reader().use(JsonParser::parseReader)

internal fun InputStream.parseJson(): JsonElement = use { JsonParser.parseReader(InputStreamReader(this)) }

internal fun JsonObject.hasString(property: String) =
    has(property) && this[property].isString()

internal fun JsonObject.hasNumber(property: String) =
    has(property) && this[property].isNumber()

internal fun JsonObject.hasBoolean(property: String) =
    has(property) && this[property].isBoolean()

internal fun JsonObject.hasObject(property: String) =
    has(property) && this[property] is JsonObject

internal fun JsonObject.hasArray(property: String) =
    has(property) && this[property] is JsonArray

internal fun JsonObject.getString(property: String) = if (hasString(property)) get(property).asString else null

internal fun JsonObject.getNumber(property: String) = if (hasNumber(property)) get(property).asNumber else null

internal fun JsonObject.getInt(property: String) = if (hasNumber(property)) get(property).asInt else null

internal fun JsonObject.getLong(property: String) = if (hasNumber(property)) get(property).asLong else null

internal fun JsonObject.getDouble(property: String) = if (hasNumber(property)) get(property).asDouble else null

internal fun JsonObject.getFloat(property: String) = if (hasNumber(property)) get(property).asFloat else null

internal fun JsonObject.getBoolean(property: String) = if (hasBoolean(property)) get(property).asBoolean else null

internal fun JsonObject.getString(property: String, default: String): String = if (hasString(property)) get(property).asString else default

internal fun JsonObject.getNumber(property: String, default: Number): Number = if (hasNumber(property)) get(property).asNumber else default

internal fun JsonObject.getInt(property: String, default: Int) = if (hasNumber(property)) get(property).asInt else default

internal fun JsonObject.getDouble(property: String, default: Double) = if (hasNumber(property)) get(property).asDouble else default

internal fun JsonObject.getFloat(property: String, default: Float) = if (hasNumber(property)) get(property).asFloat else default

internal fun JsonObject.getBoolean(property: String, default: Boolean) = if (hasBoolean(property)) get(property).asBoolean else default

internal fun JsonObject.getOrNull(property: String) = if (has(property)) get(property) else null

internal operator fun JsonObject.set(property: String, value: JsonElement) = add(property, value)

internal fun JsonElement.writeToFile(file: File) =
    file.writeText(toString())

internal fun JsonElement.isString() =
    this is JsonPrimitive && isString

internal fun JsonElement.isBoolean() =
    this is JsonPrimitive && isBoolean

internal fun JsonElement.isNumber() =
    this is JsonPrimitive && isNumber

internal fun JsonArray.addAll(vararg numbers: Number) =
    numbers.forEach(this::add)

internal fun JsonArray.addAll(vararg booleans: Boolean) =
    booleans.forEach(this::add)

internal fun JsonArray.addAll(vararg chars: Char) =
    chars.forEach(this::add)

internal fun JsonArray.addAll(vararg strings: String) =
    strings.forEach(this::add)

internal fun JsonArray.addAll(vararg elements: JsonElement) =
    elements.forEach(this::add)

internal fun JsonArray.addAll(intArray: IntArray) =
    intArray.forEach(this::add)

internal fun JsonArray.addAll(longArray: LongArray) =
    longArray.forEach(this::add)

internal fun JsonArray.addAll(floatArray: FloatArray) =
    floatArray.forEach(this::add)

internal fun JsonArray.addAll(doubleArray: DoubleArray) =
    doubleArray.forEach(this::add)

@JvmName("addAllBooleanArray")
internal fun JsonArray.addAll(booleanArray: BooleanArray) =
    booleanArray.forEach(this::add)

@JvmName("addAllCharArray")
internal fun JsonArray.addAll(charArray: CharArray) =
    charArray.forEach(this::add)

@JvmName("addAllStringArray")
internal fun JsonArray.addAll(stringArray: Array<String>) =
    stringArray.forEach(this::add)

@JvmName("addAllElementsArray")
internal fun JsonArray.addAll(elementArray: Array<JsonElement>) =
    elementArray.forEach(this::add)

@JvmName("addAllNumbers")
internal fun JsonArray.addAll(numbers: Iterable<Number>) =
    numbers.forEach(this::add)

@JvmName("addAllBooleans")
internal fun JsonArray.addAll(booleans: Iterable<Boolean>) =
    booleans.forEach(this::add)

@JvmName("addAllChars")
internal fun JsonArray.addAll(chars: Iterable<Char>) =
    chars.forEach(this::add)

@JvmName("addAllStrings")
internal fun JsonArray.addAll(strings: Iterable<String>) =
    strings.forEach(this::add)

@JvmName("addAllElements")
internal fun JsonArray.addAll(elements: Iterable<JsonElement>) =
    elements.forEach(this::add)

internal fun JsonArray.getAllStrings() =
    filter(JsonElement::isString).map { it.asString }

internal fun <T : MutableCollection<String>> JsonArray.getAllStringsTo(destination: T) =
    filter(JsonElement::isString).mapTo(destination) { it.asString }

internal fun JsonArray.getAllDoubles() =
    filter(JsonElement::isNumber).map { it.asDouble }

internal fun JsonArray.getAllInts() =
    filter(JsonElement::isNumber).map { it.asInt }

internal fun JsonArray.getAllJsonObjects() =
    filterIsInstance<JsonObject>()

internal fun <T> JsonArray.toStringList(consumer: (List<String>) -> T) =
    consumer(this.filter(JsonElement::isString).map(JsonElement::getAsString))

internal fun JsonObject.addAll(other: JsonObject) {
    other.entrySet().forEach { (property, value) -> add(property, value) }
}

internal inline fun <reified T> Gson.fromJson(json: String?): T? {
    if (json == null) return null
    return fromJson(json, type<T>())
}

internal inline fun <reified T> Gson.fromJson(jsonElement: JsonElement?): T? {
    if (jsonElement == null) return null
    return fromJson(jsonElement, type<T>())
}

internal inline fun <reified T> Gson.fromJson(reader: Reader): T? {
    return fromJson(reader, type<T>())
}

internal inline fun <reified T> GsonBuilder.registerTypeHierarchyAdapter(typeAdapter: Any): GsonBuilder =
    registerTypeHierarchyAdapter(T::class.java, typeAdapter)

internal inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type