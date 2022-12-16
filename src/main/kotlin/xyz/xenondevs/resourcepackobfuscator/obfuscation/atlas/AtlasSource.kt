package xyz.xenondevs.resourcepackobfuscator.obfuscation.atlas

import com.google.gson.JsonObject
import xyz.xenondevs.resourcepackobfuscator.ResourcePath
import xyz.xenondevs.resourcepackobfuscator.json.reference
import xyz.xenondevs.resourcepackobfuscator.json.referenceNullable
import xyz.xenondevs.resourcepackobfuscator.util.getString

internal sealed interface AtlasSource {
    
    val isMutable: Boolean
    
    fun includes(path: ResourcePath): Boolean
    fun excludes(path: ResourcePath): Boolean
    
    companion object {
        
        fun fromJson(obj: JsonObject, isMutable: Boolean): AtlasSource =
            when (val type = obj.getString("type")!!) {
                "directory" -> DirectorySource(obj, isMutable)
                "single" -> SingleSource(obj, isMutable)
                "filter" -> FilterSource(obj, isMutable)
                // TODO: UnstitchSource
                else -> throw UnsupportedOperationException("Unsupported source type: $type")
            }
        
    }
    
}

internal class DirectorySource(obj: JsonObject, override val isMutable: Boolean) : AtlasSource {
    
    var source: String by obj.reference("source")
    var prefix: String by obj.reference("prefix")
    
    override fun includes(path: ResourcePath): Boolean =
        path.path.startsWith("textures/$prefix")
    
    override fun excludes(path: ResourcePath) = false
    
}

internal class SingleSource(obj: JsonObject, override val isMutable: Boolean) : AtlasSource {
    
    var resource: ResourcePath by obj.reference("resource")
    var sprite: ResourcePath? by obj.referenceNullable("sprite")
    
    override fun includes(path: ResourcePath): Boolean = path == resource
    override fun excludes(path: ResourcePath): Boolean = false
    
}

internal data class FilterSource(val obj: JsonObject, override val isMutable: Boolean) : AtlasSource {
    
    var namespace: Regex by obj.reference("namespace")
    var path: Regex by obj.reference("path")
    
    override fun includes(path: ResourcePath): Boolean = false
    
    override fun excludes(path: ResourcePath): Boolean {
        return this.namespace.matches(path.namespace) && this.path.matches(path.path)
    }
    
}