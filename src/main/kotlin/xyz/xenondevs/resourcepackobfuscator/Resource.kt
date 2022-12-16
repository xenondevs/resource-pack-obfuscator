package xyz.xenondevs.resourcepackobfuscator

private val NAMESPACED_PATH_REGEX = Regex("""^(\w*):([\w/-]*)$""")
private val NAMESPACE_REGEX = Regex("""^(\w*)$""")
private val PATH_REGEX = Regex("""^([\w/-]*)$""")

internal data class ResourcePath(val namespace: String, val path: String) {
    
    private val id = "$namespace:$path"
    
    init {
        require(NAMESPACE_REGEX.matches(namespace)) { "Illegal namespace: $namespace" }
        require(PATH_REGEX.matches(path)) { "Illegal path: $path" }
    }
    
    override fun equals(other: Any?): Boolean {
        return other is ResourcePath && other.id == id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return id
    }
    
    companion object {
        
        fun of(id: String, fallbackNamespace: String = "minecraft"): ResourcePath {
            return if (PATH_REGEX.matches(id)) {
                ResourcePath(fallbackNamespace, id)
            } else {
                val match = NAMESPACED_PATH_REGEX.matchEntire(id)
                    ?: throw IllegalArgumentException("Invalid resource id: $id")
                
                ResourcePath(match.groupValues[1], match.groupValues[2])
            }
        }
        
    }
    
}