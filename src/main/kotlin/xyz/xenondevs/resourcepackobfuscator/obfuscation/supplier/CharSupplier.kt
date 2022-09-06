package xyz.xenondevs.resourcepackobfuscator.obfuscation.supplier

import kotlin.math.ceil
import kotlin.math.log

private val UINT_MAX = UInt.MAX_VALUE.toDouble()

internal class CharSupplier(private val chars: CharArray) {
    
    private val maxLength = ceil(log(UINT_MAX, chars.size.toDouble())).toInt()
    private var index = -1
    
    fun nextString(): String = stringAt(++index)
    
    private fun stringAt(index: Int): String {
        val charsLength = chars.size
        var i = index
        val buf = CharArray(maxLength + 1)
        var charPos = maxLength
        var t = false
        
        if (i > 0) i = -i
        
        while (i <= -charsLength) {
            t = true
            buf[charPos--] = chars[-(i % charsLength)]
            i /= charsLength
        }
        i = if (t) -(i + 1) else -i
        buf[charPos] = chars[i]
        
        return String(buf, charPos, maxLength + 1 - charPos)
    }
    
}