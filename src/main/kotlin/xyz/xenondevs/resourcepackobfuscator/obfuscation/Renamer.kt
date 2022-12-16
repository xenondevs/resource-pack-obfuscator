package xyz.xenondevs.resourcepackobfuscator.obfuscation

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import xyz.xenondevs.resourcepackobfuscator.ResourcePath
import xyz.xenondevs.resourcepackobfuscator.obfuscation.atlas.AtlasSource
import xyz.xenondevs.resourcepackobfuscator.obfuscation.atlas.AtlasSources
import xyz.xenondevs.resourcepackobfuscator.obfuscation.supplier.CharSupplier
import xyz.xenondevs.resourcepackobfuscator.util.GSON
import xyz.xenondevs.resourcepackobfuscator.util.getOrNull
import xyz.xenondevs.resourcepackobfuscator.util.getString
import xyz.xenondevs.resourcepackobfuscator.util.parseJson
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeTo

private val CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()

internal class Renamer(private val packDir: Path, private val mcassetsDir: Path) {
    
    private val assetsDir = packDir.resolve("assets/")
    private val nameSupplier = CharSupplier(CHARS)
    
    private val atlases = AtlasSources(packDir, mcassetsDir)
    private val mappings = HashMap<String, String>()
    private val textureIdMappings = HashMap<ResourcePath, ResourcePath>()
    private val modelIdMappings = HashMap<ResourcePath, ResourcePath>()
    private val soundIdMappings = HashMap<ResourcePath, ResourcePath>()
    
    private val obfNamespace = nameSupplier.nextString()
    
    //<editor-fold desc="Mappings generation logic">
    fun createNameMappings() {
        assetsDir.listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach {
                val namespace = it.name
                createTextureNameMappings(namespace, it.resolve("textures"), textureIdMappings)
                createNameMappings(namespace, it.resolve("models"), modelIdMappings)
                createNameMappings(namespace, it.resolve("sounds"), soundIdMappings)
            }
    }
    
    fun getNewFilePath(file: Path): String {
        val relPath = file.relativeTo(packDir).invariantSeparatorsPathString
        return mappings[relPath] ?: relPath
    }
    
    private fun createTextureNameMappings(
        namespace: String,
        folder: Path,
        mappingsMap: MutableMap<ResourcePath, ResourcePath>
    ) {
        walkAssetsFolder(namespace, folder) { file, relPathToPack, resourcePath ->
            val obfName: String
            val obfPath: ResourcePath
            
            val singleSources = atlases.getSingleFileSources(resourcePath)
            val dirSources = atlases.getDirectorySources(resourcePath)
            if (singleSources.isNotEmpty()) {
                if (singleSources.all(AtlasSource::isMutable)) {
                    obfName = nameSupplier.nextString()
                    obfPath = ResourcePath(obfNamespace, obfName)
                    singleSources.forEach {
                        it.resource = obfPath
                        
                        // obfuscate the separate sprite resource path, if present
                        val spritePath = it.sprite
                        if (spritePath != null) {
                            val obfSpriteName = nameSupplier.nextString()
                            val obfSpritePath = ResourcePath(obfNamespace, obfSpriteName)
                            mappingsMap[spritePath] = obfSpritePath
                        }
                    }
                } else {
                    // do not rename the texture if non-mutable atlas sources reference it
                    return@walkAssetsFolder
                }
            } else if (dirSources.isNotEmpty()) {
                // TODO: obfuscate directory names
                // the directory with the longest name is the most specific one that fits for all atlases
                val commonDirPrefix = dirSources.maxBy { it.prefix.length }.prefix
                obfName = commonDirPrefix + nameSupplier.nextString()
                obfPath = ResourcePath(obfNamespace, obfName)
            } else {
                obfName = nameSupplier.nextString()
                obfPath = ResourcePath(obfNamespace, obfName)
            }
            
            // resource path mapping
            mappingsMap[resourcePath] = obfPath
            // file path mapping
            val newFilePath = "assets/$obfNamespace/${folder.name}/$obfName.png"
            mappings[relPathToPack] = newFilePath
            // mcmeta file path mapping
            val mcMetaFile = file.parent.resolve(file.name + ".mcmeta")
            if (mcMetaFile.exists())
                mappings["$relPathToPack.mcmeta"] = "$newFilePath.mcmeta"
        }
        
        atlases.writeMutableSources()
    }
    
    private fun createNameMappings(
        namespace: String,
        folder: Path,
        mappingsMap: MutableMap<ResourcePath, ResourcePath>
    ) {
        walkAssetsFolder(namespace, folder) { file, relPathToPack, resourcePath ->
            val name = nameSupplier.nextString()
            val nameWithExt = "$name.${file.extension}"
            
            // resource path mapping
            mappingsMap[resourcePath] = ResourcePath(obfNamespace, name)
            // file path mapping
            val newFilePath = "assets/$obfNamespace/${folder.name}/$nameWithExt"
            mappings[relPathToPack] = newFilePath
        }
    }
    
    private fun walkAssetsFolder(
        namespace: String,
        folder: Path,
        walk: (Path, String, ResourcePath) -> Unit
    ) {
        if (!folder.exists())
            return
        
        Files.walk(folder).forEach { file ->
            if (file.isDirectory() || file.extension == "mcmeta")
                return@forEach
            
            val relPathToPack = file.relativeTo(packDir).invariantSeparatorsPathString
            val relPathToFolder = file.relativeTo(folder).invariantSeparatorsPathString
            val resourcePath = ResourcePath(namespace, relPathToFolder.substringBeforeLast('.'))
            if (!isDefaultAsset(relPathToPack)) {
                walk(file, relPathToPack, resourcePath)
            }
        }
    }
    
    private fun isDefaultAsset(path: String) = mcassetsDir.resolve(path).exists()
    //</editor-fold>
    
    //<editor-fold desc="Mappings applying logic">
    fun processAndCopyFile(file: Path, out: OutputStream) {
        val json = file.parseJson()
        
        if (json is JsonObject) {
            // check for sounds file
            if (file.name.equals("sounds.json", true) && file.parent.parent.name.equals("assets", true)) {
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
    
    private fun getNewTextureResourceId(id: String): ResourcePath {
        val rid = ResourcePath.of(id)
        return textureIdMappings[rid] ?: rid
    }
    
    private fun getNewModelResourceId(id: String): ResourcePath {
        val rid = ResourcePath.of(id)
        return modelIdMappings[rid] ?: rid
    }
    
    private fun getNewSoundResourceId(id: String): ResourcePath {
        val rid = ResourcePath.of(id)
        return soundIdMappings[rid] ?: rid
    }
    //</editor-fold>
    
}