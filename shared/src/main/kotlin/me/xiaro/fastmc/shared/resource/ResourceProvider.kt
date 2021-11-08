package me.xiaro.fastmc.shared.resource

class ResourceProvider<T : Resource>(vararg resources: T) {
    internal val resourceMap = HashMap<String, T>()

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
}