package me.luna.fastmc.mixin

import net.minecraft.util.math.Matrix3f

interface IPatchedMatrix3f {
    fun scale(x: Float, y: Float, z: Float)
    fun set(other: Matrix3f)
}