package me.luna.fastmc.shared.resource

interface Resource {
    val resourceName: String

    fun destroy()
}