package me.luna.fastmc.shared.resource

class ResourceProvider<T : Resource>(private val resourceClass: Class<T>, vararg resources: T) {
    private val resourceMap = HashMap<String, T>()

    operator fun get(name: String): T {
        return resourceMap[name] ?: throw IllegalArgumentException("Invalid resource $name($resourceClass)")
    }

    val resources: Collection<T>
        get() = resourceMap.values

    fun destroy() {
        resources.forEach {
            it.destroy()
        }
    }

    init {
        resources.forEach {
            resourceMap[it.resourceName] = it
        }
    }

    companion object {
        inline operator fun <reified T : Resource> invoke(vararg resources: T): ResourceProvider<T> {
            return ResourceProvider(T::class.java, *resources)
        }
    }
}