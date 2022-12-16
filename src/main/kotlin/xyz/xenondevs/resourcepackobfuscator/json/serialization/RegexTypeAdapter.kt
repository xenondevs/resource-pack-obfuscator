package xyz.xenondevs.resourcepackobfuscator.json.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal object RegexTypeAdapter : TypeAdapter<Regex>() {
    
    override fun write(writer: JsonWriter, value: Regex) {
        writer.value(value.pattern)
    }
    
    override fun read(reader: JsonReader): Regex {
        return Regex(reader.nextString())
    }
    
}