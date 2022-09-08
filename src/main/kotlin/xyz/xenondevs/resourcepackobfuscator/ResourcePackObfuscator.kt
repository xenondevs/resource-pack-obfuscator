package xyz.xenondevs.resourcepackobfuscator

import kotlinx.coroutines.runBlocking
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.resourcepackobfuscator.obfuscation.Renamer
import xyz.xenondevs.resourcepackobfuscator.protection.CorruptingZipOutputStream
import java.io.File
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackObfuscator(
    private val obfuscate: Boolean,
    private val corruptEntries: Boolean,
    private val packDir: File,
    private val packZip: File,
    private val mcassetsDir: File
) {
    
    fun packZip() {
        val renamer: Renamer?
        
        if (obfuscate) {
            if (!mcassetsDir.exists()) {
                runBlocking {
                    MinecraftAssetsDownloader(outputDirectory = mcassetsDir, mode = ExtractionMode.GITHUB)
                        .downloadAssets()
                }
            }
            
            renamer = Renamer(packDir, mcassetsDir)
            renamer.createNameMappings()
        } else {
            renamer = null
        }
        
        ZipOutputStream(packZip.outputStream().let { if (corruptEntries) CorruptingZipOutputStream(it) else it }).use { out ->
            packDir.walkTopDown().forEach { file ->
                if (file.isDirectory)
                    return@forEach
                
                val path = (renamer?.getNewFilePath(file) ?: file.relativeTo(packDir).invariantSeparatorsPath)
                
                val entry = ZipEntry(if (corruptEntries) "$path/" else path)
                entry.creationTime = FileTime.fromMillis(0L)
                entry.lastAccessTime = FileTime.fromMillis(0L)
                entry.lastModifiedTime = FileTime.fromMillis(0L)
                
                out.putNextEntry(entry)
                if (renamer != null && file.extension == "json") {
                    renamer.processAndCopyFile(file, out)
                } else file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
            
            out.flush()
        }
    }
    
}