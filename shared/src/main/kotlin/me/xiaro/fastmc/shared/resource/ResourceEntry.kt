package me.xiaro.fastmc.shared.resource

interface ResourceEntry<T : Resource> {
    fun get(resourceManager: IResourceManager): T

    companion object {
        operator fun <T : Resource> invoke(
            resourceClass: Class<T>,
            name: String,
            block: (IResourceManager) -> ResourceProvider<T>
        ): ResourceEntry<T> {
            return EntryInternal(resourceClass, name, block)
        }
    }

    private class EntryInternal<T : Resource>(
        private val resourceClass: Class<T>,
        private val name: String,
        private val block: (IResourceManager) -> ResourceProvider<T>
    ) : ResourceEntry<T> {
        var source: ResourceProvider<T>? = null
        var value: T? = null

        override fun get(resourceManager: IResourceManager): T {
            val source = block.invoke(resourceManager)

            return if (source !== this.source) {
                this.source = source
                source.resourceMap[name]!!.also { value = it }
            } else {
                value ?: source.resourceMap[name]!!.also { value = it }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EntryInternal<*>

            if (name != other.name) return false
            if (resourceClass != other.resourceClass) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + resourceClass.hashCode()
            return result
        }
    }
}