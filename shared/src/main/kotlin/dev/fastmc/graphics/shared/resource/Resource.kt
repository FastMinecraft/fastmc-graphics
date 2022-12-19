package dev.fastmc.graphics.shared.resource

interface Resource {
    val resourceName: String

    fun destroy()
}