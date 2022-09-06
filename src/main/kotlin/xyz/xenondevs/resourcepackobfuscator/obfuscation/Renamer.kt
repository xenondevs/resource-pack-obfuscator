package xyz.xenondevs.resourcepackobfuscator.obfuscation

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import xyz.xenondevs.resourcepackobfuscator.ResourceId
import xyz.xenondevs.resourcepackobfuscator.obfuscation.supplier.CharSupplier
import xyz.xenondevs.resourcepackobfuscator.util.GSON
import xyz.xenondevs.resourcepackobfuscator.util.getOrNull
import xyz.xenondevs.resourcepackobfuscator.util.getString
import xyz.xenondevs.resourcepackobfuscator.util.parseJson
import java.io.File
import java.io.OutputStream

private val CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()

internal class Renamer(private val packDir: File, private val mcassetsDir: File) {
    
    private val assetsDir = File(packDir, "assets/")
    private val nameSupplier = CharSupplier(CHARS)
    
    private val mappings = HashMap<String, String>()
    private val textureIdMappings = HashMap<ResourceId, ResourceId>()
    private val modelIdMappings = HashMap<ResourceId, ResourceId>()
    private val soundIdMappings = HashMap<ResourceId, ResourceId>()
    
    private val obfNamespace = nameSupplier.nextString()
    
    //<editor-fold desc="Mappings generation logic">
    fun createNameMappings() {
        assetsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach {
                val namespace = it.name
                createNameMappings(namespace, File(it, "textures"), textureIdMappings)
                createNameMappings(namespace, File(it, "models"), modelIdMappings)
                createNameMappings(namespace, File(it, "sounds"), soundIdMappings)
            }
    }
    
    fun getNewFilePath(file: File): String {
        val relPath = file.relativeTo(packDir).invariantSeparatorsPath
        return mappings[relPath] ?: relPath
    }
    
    private fun createNameMappings(
        namespace: String,
        folder: File,
        mappingsMap: MutableMap<ResourceId, ResourceId>
    ) {
        folder.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension == "mcmeta")
                return@forEach
            
            val relPathToPack = file.relativeTo(packDir).invariantSeparatorsPath
            if (!isDefaultAsset(relPathToPack)) {
                val name = nameSupplier.nextString()
                val nameWithExt = "$name.${file.extension}"
                
                // id mapping
                val id = ResourceId(
                    namespace,
                    file.relativeTo(folder)
                        .invariantSeparatorsPath
                        .substringBeforeLast('.')
                )
                mappingsMap[id] = ResourceId(obfNamespace, name)
                // file path mapping
                val newFilePath = "assets/$obfNamespace/${folder.name}/$nameWithExt"
                mappings[relPathToPack] = newFilePath
                // mcmeta file path mapping
                val mcMetaFile = File(file.parentFile, file.name + ".mcmeta")
                if (mcMetaFile.exists())
                    mappings["$relPathToPack.mcmeta"] = "$newFilePath.mcmeta"
            }
        }
    }
    
    private fun isDefaultAsset(path: String) = File(mcassetsDir, path).exists()
    //</editor-fold>
    
    //<editor-fold desc="Mappings applying logic">
    fun processAndCopyFile(file: File, out: OutputStream) {
        val json = file.parseJson()
        
        if (json is JsonObject) {
            // check for sounds file
            if (file.name.equals("sounds.json", true) && file.parentFile.parentFile.name.equals("assets", true)) {
                renameSounds(json)
            } else {
                val keys = json.keySet()
                when {
                    "parent" in keys || "textures" in keys || "overrides" in keys -> renameModel(json)
                    "multipart" in keys || "variants" in keys -> renameBlockState(json)
                    "providers" in keys -> renameFont(json)
                }
            }
        }
        
        out.write(GSON.toJson(json).encodeToByteArray())
    }
    
    private fun renameSounds(json: JsonObject) {
        json.entrySet().forEach { (_, obj) ->
            if (obj !is JsonObject)
                return@forEach
            
            val sounds = obj.getOrNull("sounds")
            if (sounds is JsonArray) {
                sounds.withIndex().forEach { (idx, sound) ->
                    if (sound is JsonPrimitive && sound.isString) {
                        sounds.set(idx, JsonPrimitive(getNewSoundResourceId(sound.asString).toString()))
                    } else if (sound is JsonObject) {
                        sound.updateSoundProperty("name")
                    }
                }
            }
        }
    }
    
    private fun renameModel(json: JsonObject) {
        // parent
        json.updateModelProperty("parent")
        // textures
        val textures = json.get("textures") as? JsonObject
        textures?.entrySet()?.forEach { (key, value) ->
            if (!value.asString.startsWith("#"))
                textures.updateTextureProperty(key)
        }
        // overrides
        (json.get("overrides") as? JsonArray)
            ?.filterIsInstance<JsonObject>()
            ?.forEach { it.updateModelProperty("model") }
    }
    
    private fun renameBlockState(json: JsonObject) {
        (json.getOrNull("multipart") as? JsonArray)?.let(::renameMultipartBlockState)
        (json.getOrNull("variants") as? JsonObject)?.let(::renameVariantsBlockState)
    }
    
    private fun renameMultipartBlockState(multipart: JsonArray) {
        multipart.forEach { obj ->
            if (obj !is JsonObject)
                return@forEach
            
            val apply = obj.getOrNull("apply")
            if (apply is JsonObject)
                apply.updateModelProperty("model")
        }
    }
    
    private fun renameVariantsBlockState(variants: JsonObject) {
        variants.entrySet().forEach { (_, obj) ->
            if (obj !is JsonObject)
                return@forEach
            
            obj.updateModelProperty("model")
        }
    }
    
    private fun renameFont(json: JsonObject) {
        val providers = json.getOrNull("providers")
        if (providers is JsonArray) {
            providers.forEach { obj ->
                if (obj !is JsonObject)
                    return@forEach
                
                val providerFile = obj.getString("file")
                    ?: return@forEach
                
                obj.addProperty(
                    "file",
                    getNewTextureResourceId(
                        providerFile.removeSuffix(".png")
                    ).toString() + ".png"
                )
            }
        }
    }
    
    private fun JsonObject.updateModelProperty(key: String) {
        val value = getString(key)
        if (value != null)
            addProperty(key, getNewModelResourceId(value).toString())
    }
    
    private fun JsonObject.updateTextureProperty(key: String) {
        val value = getString(key)
        if (value != null)
            addProperty(key, getNewTextureResourceId(value).toString())
    }
    
    private fun JsonObject.updateSoundProperty(key: String) {
        val value = getString(key)
        if (value != null)
            addProperty(key, getNewSoundResourceId(value).toString())
    }
    
    private fun getNewTextureResourceId(id: String): ResourceId {
        val rid = ResourceId.of(id)
        return textureIdMappings[rid] ?: rid
    }
    
    private fun getNewModelResourceId(id: String): ResourceId {
        val rid = ResourceId.of(id)
        return modelIdMappings[rid] ?: rid
    }
    
    private fun getNewSoundResourceId(id: String): ResourceId {
        val rid = ResourceId.of(id)
        return soundIdMappings[rid] ?: rid
    }
    //</editor-fold>
    
}