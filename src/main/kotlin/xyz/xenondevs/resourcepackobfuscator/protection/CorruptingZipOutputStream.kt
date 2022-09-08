package xyz.xenondevs.resourcepackobfuscator.protection

import java.io.OutputStream

private val ZIP_HEADER_START = intArrayOf(0x50, 0x4B)
private val ZIP_END_SIGNATURE = intArrayOf(0x50, 0x4B, 0x05, 0x06)

internal class CorruptingZipOutputStream(private val out: OutputStream) : OutputStream() {
    
    private var skip = 0
    private var write = true
    
    private var overwriteLength = 0
    private var overwrite = false
    
    private var headerIdx = 0
    private var currentSignature = ByteArray(2)
    
    init {
        out.write(ZIP_END_SIGNATURE[0])
        out.write(ZIP_END_SIGNATURE[1])
        out.write(ZIP_END_SIGNATURE[2])
        out.write(ZIP_END_SIGNATURE[3])
    }
    
    override fun write(b: Int) {
        if (skip > 0) {
            skip--
            
            if (write)
                out.write(b)
            
            if (skip == 0)
                write = true
            
            return
        }
        
        if (overwrite) {
            repeat(overwriteLength) { out.write(0xFF) }
            skip = overwriteLength - 1
            write = false
            overwrite = false
            
            return
        }
        
        if (headerIdx > 1) {
            currentSignature[headerIdx - 2] = b.toByte()
            headerIdx++
            
            if (headerIdx == 4) {
                headerIdx = 0
                
                val header = HeaderType.values().firstOrNull { it.signature.contentEquals(currentSignature) }
                if (header != null) {
                    skip = header.skip
                    overwrite = true
                    overwriteLength = header.overwrite
                }
            }
        } else if (b == ZIP_HEADER_START[headerIdx]) {
            headerIdx++
        } else headerIdx = 0
        
        out.write(b)
    }
    
    private enum class HeaderType(val skip: Int, val overwrite: Int, vararg val signature: Byte) {
        CENTRAL_DIRECTORY(12, 12, 0x01, 0x02),
        LOCAL_FILE(10, 12, 0x03, 0x04),
        // END(0, 18, 0x05, 0x06) fixme
    }
    
}