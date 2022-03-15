package me.luna.fastmc.shared.util.collection

class ClassInheritanceMap<K : Class<*>, V>(private val baseClass: K, private val wrapped: MutableMap<K, V>) :
    MutableMap<K, V> by wrapped {
    @Suppress("UNCHECKED_CAST")
    override fun containsKey(key: K): Boolean {
        var value = wrapped[key]

        if (value == null && key != baseClass) {
            value = this[key.superclass as K]
            if (value != null) this[key] = value
        }

        return value != null
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(key: K): V? {
        var value = wrapped[key]

        if (value == null && key != baseClass) {
            value = this[key.superclass as K]
            if (value != null) this[key] = value
        }

        return value
    }
}