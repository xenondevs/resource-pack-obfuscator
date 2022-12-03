package xyz.xenondevs.resourcepackobfuscator

import kotlinx.coroutines.runBlocking
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.resourcepackobfuscator.obfuscation.Renamer
import xyz.xenondevs.resourcepackobfuscator.protection.CorruptingZipOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

class ResourcePackObfuscator(
    private val obfuscate: Boolean,
    private val corruptEntries: Boolean,
    private val packDir: Path,
    private val packZip: Path,
    private val mcassetsDir: File,
    private val fileFilter: ((Path) -> Boolean)? = null
) {
    
    constructor(
        obfuscate: Boolean,
        corruptEntries: Boolean,
        packDir: File,
        packZip: File,
        mcassetsDir: File,
        fileFilter: ((Path) -> Boolean)? = null
    ) : this(obfuscate, corruptEntries, packDir.toPath(), packZip.toPath(), mcassetsDir, fileFilter)
    
    fun packZip(compressionLevel: Int = Deflater.DEFAULT_COMPRESSION) {
        val renamer: Renamer?
        
        if (obfuscate) {
            if (!mcassetsDir.exists()) {
                runBlocking {
                    MinecraftAssetsDownloader(outputDirectory = mcassetsDir, mode = ExtractionMode.GITHUB)
                        .downloadAssets()
                }
            }
            
            renamer = Renamer(packDir, mcassetsDir.toPath())
            renamer.createNameMappings()
        } else {
            renamer = null
        }
        
        ZipOutputStream(
            packZip.outputStream().let { if (corruptEntries) CorruptingZipOutputStream(it) else it }
        ).use { out ->
            out.setLevel(compressionLevel)
            
            Files.walk(packDir).forEach { file ->
                if (file.isDirectory() || fileFilter?.invoke(file) == true)
                    return@forEach
                
                val path = (renamer?.getNewFilePath(file) ?: file.relativeTo(packDir).invariantSeparatorsPathString)
                
                val entry = ZipEntry(if (corruptEntries && !isFontPath(path)) "$path/" else path)
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
    
    private fun isFontPath(path: String): Boolean =
        path.substringAfter('/')
            .substringAfter('/')
            .substringBefore('/')
            .equals("font", true)
    
}