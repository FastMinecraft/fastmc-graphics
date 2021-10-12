package me.xiaro.fastmc.resource

interface Resource {
    val resourceName: String

    fun destroy()
}