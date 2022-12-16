package xyz.xenondevs.resourcepackobfuscator.json.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.resourcepackobfuscator.ResourcePath

internal object ResourcePathTypeAdapter : TypeAdapter<ResourcePath>() {
    
    override fun write(writer: JsonWriter, value: ResourcePath) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): ResourcePath {
        return ResourcePath.of(reader.nextString())
    }
    
}