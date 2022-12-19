package dev.fastmc.graphics.shared.instancing

interface IInfo<E : Any> {
    val entity: E
    val typeID: Int
}