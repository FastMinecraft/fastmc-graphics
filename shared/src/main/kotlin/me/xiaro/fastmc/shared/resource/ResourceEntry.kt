package me.xiaro.fastmc.shared.resource

interface ResourceEntry<T : Resource> {
    fun get(resourceManager: IResourceManager): T

    companion object {
        operator fun <T : Resource> invoke(
            name: String,
            block: (IResourceManager) -> ResourceProvider<T>
        ): ResourceEntry<T> {
            return EntryInternal(name, block)
        }
    }

    private class EntryInternal<T : Resource>(
        private val name: String,
        private val block: (IResourceManager) -> ResourceProvider<T>
    ) : ResourceEntry<T> {
        var source: ResourceProvider<T>? = null
        var value: T? = null

        override fun get(resourceManager: IResourceManager): T {
            val source = block.invoke(resourceManager)

            return if (source !== this.source) {
                this.source = source
                source[name].also { value = it }
            } else {
                value ?: source[name].also { value = it }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EntryInternal<*>

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}