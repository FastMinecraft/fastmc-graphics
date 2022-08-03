package me.luna.fastmc.shared.instancing

interface IInfo<E : Any> {
    val entity: E
    val typeID: Int
}