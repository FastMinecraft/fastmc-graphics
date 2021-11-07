package me.xiaro.fastmc

import me.xiaro.fastmc.accessor.AccessorMatrix4f
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.profiler.Profiler
import net.minecraft.world.World

fun Matrix4f.toJoml(): org.joml.Matrix4f {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val accessor = this as AccessorMatrix4f

    return org.joml.Matrix4f(
        accessor.a00,
        accessor.a01,
        accessor.a02,
        accessor.a03,
        accessor.a10,
        accessor.a11,
        accessor.a12,
        accessor.a13,
        accessor.a20,
        accessor.a21,
        accessor.a22,
        accessor.a23,
        accessor.a30,
        accessor.a31,
        accessor.a32,
        accessor.a33,
    )
}

fun Profiler.startSection(name: String) {
    this.push(name)
}

fun Profiler.endSection() {
    this.pop()
}

fun Profiler.endStartSection(name: String) {
    this.swap(name)
}

val Minecraft.renderViewEntity: Entity?
    get() = this.cameraEntity

val World.loadedTileEntityList: List<TileEntity>
    get() = this.blockEntities

val Entity.lastTickPosX: Double
    get() = this.prevX

val Entity.lastTickPosY: Double
    get() = this.prevY

val Entity.lastTickPosZ: Double
    get() = this.prevZ

val Entity.posX: Double
    get() = this.x

val Entity.posY: Double
    get() = this.y

val Entity.posZ: Double
    get() = this.z

typealias Minecraft = MinecraftClient
typealias BlockChest = ChestBlock

typealias TileEntity = BlockEntity
typealias TileEntityBed = BedBlockEntity
typealias TileEntityChest = ChestBlockEntity
typealias TileEntityEnderChest = EnderChestBlockEntity
typealias TileEntityShulkerBox = ShulkerBoxBlockEntity

typealias ResourceLocation = Identifier