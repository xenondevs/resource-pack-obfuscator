package xyz.xenondevs.resourcepackobfuscator.obfuscation.atlas

import com.google.gson.JsonObject
import xyz.xenondevs.resourcepackobfuscator.ResourcePath
import xyz.xenondevs.resourcepackobfuscator.util.parseJson
import xyz.xenondevs.resourcepackobfuscator.util.writeToFile
import java.nio.file.Path
import kotlin.io.path.exists

private val ATLAS_NAMES = listOf("banner_patterns", "beds", "blocks", "chests", "mob_effects", "paintings", "particles", "shield_patterns", "shulker_boxes", "signs")

// TODO: Check for FilterSource
internal class AtlasSources(packDir: Path, mcassetsDir: Path) {
    
    private val mutableAtlasesJson = HashMap<Path, JsonObject>()
    private val atlases = HashMap<Path, ArrayList<AtlasSource>>()
    
    init {
        ATLAS_NAMES.forEach { atlasName ->
            fun loadAtlas(path: Path, isMutable: Boolean) {
                val file = path.resolve("assets/minecraft/atlases/$atlasName.json")
                val obj = file
                    .takeIf(Path::exists)?.parseJson() as? JsonObject
                    ?: return
                
                if (isMutable)
                    mutableAtlasesJson[file] = obj
                
                obj.getAsJsonArray("sources").forEach { source ->
                    source as JsonObject
                    atlases.getOrPut(file, ::ArrayList) += AtlasSource.fromJson(source, isMutable)
                }
            }
            
            loadAtlas(mcassetsDir, false)
            loadAtlas(packDir, true)
        }
    }
    
    fun getDirectorySources(path: ResourcePath): List<DirectorySource> {
        return atlases.values.asSequence()
            .flatten()
            .filterIsInstance<DirectorySource>()
            .filter { path.path.startsWith(it.prefix) }
            .toList()
    }
    
    fun getSingleFileSources(path: ResourcePath): List<SingleSource> {
        return atlases.values.asSequence()
            .flatten()
            .filterIsInstance<SingleSource>()
            .filter { it.resource == path }
            .toList()
    }
    
    fun writeMutableSources() {
        mutableAtlasesJson.forEach { (path, obj) -> obj.writeToFile(path) }
    }
    
}