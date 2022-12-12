package me.luna.fastmc.mixin

interface IPatchedVoxelShape {
    fun hash(): Int {
        return this.hashCode()
    }
}