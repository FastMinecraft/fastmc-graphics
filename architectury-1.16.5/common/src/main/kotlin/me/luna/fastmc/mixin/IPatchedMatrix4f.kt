package me.luna.fastmc.mixin

import net.minecraft.util.math.Matrix4f

interface IPatchedMatrix4f {
    fun translate(x: Float, y: Float, z: Float)
    fun scale(x: Float, y: Float, z: Float)
    fun set(other: Matrix4f)
}