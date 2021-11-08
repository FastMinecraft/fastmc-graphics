package me.xiaro.fastmc.shared.resource

interface Resource {
    val resourceName: String

    fun destroy()
}