@file:Suppress("NOTHING_TO_INLINE")

package dev.fastmc.graphics.util

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.CowEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.profiler.Profiler

fun Matrix4f.toJoml(dest: org.joml.Matrix4f): org.joml.Matrix4f {
    dest.m00(this.a00)
    dest.m01(this.a10)
    dest.m02(this.a20)
    dest.m03(this.a30)
    dest.m10(this.a01)
    dest.m11(this.a11)
    dest.m12(this.a21)
    dest.m13(this.a31)
    dest.m20(this.a02)
    dest.m21(this.a12)
    dest.m22(this.a22)
    dest.m23(this.a32)
    dest.m30(this.a03)
    dest.m31(this.a13)
    dest.m32(this.a23)
    dest.m33(this.a33)

    return dest
}

fun Matrix4f.toJoml(): org.joml.Matrix4f {
    return org.joml.Matrix4f(
        this.a00,
        this.a10,
        this.a20,
        this.a30,
        this.a01,
        this.a11,
        this.a21,
        this.a31,
        this.a02,
        this.a12,
        this.a22,
        this.a32,
        this.a03,
        this.a13,
        this.a23,
        this.a33,
    )
}

inline fun Profiler.startSection(name: String) {
    this.push(name)
}

inline fun Profiler.endSection() {
    this.pop()
}

inline fun Profiler.endStartSection(name: String) {
    this.swap(name)
}

inline val Minecraft.renderViewEntity: Entity?
    get() = this.cameraEntity

typealias Minecraft = MinecraftClient
typealias BlockChest = ChestBlock

typealias TileEntity = BlockEntity
typealias TileEntityBed = BedBlockEntity
typealias TileEntityChest = ChestBlockEntity
typealias TileEntityEnderChest = EnderChestBlockEntity
typealias TileEntityShulkerBox = ShulkerBoxBlockEntity

typealias EntityCow = CowEntity

typealias ResourceLocation = Identifier