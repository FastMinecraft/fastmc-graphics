package me.luna.fastmc

import me.luna.fastmc.mixin.accessor.AccessorMatrix4f
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.CowEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.profiler.Profiler
import net.minecraft.world.World

fun Matrix4f.toJoml(): org.joml.Matrix4f {
    @Suppress("CAST_NEVER_SUCCEEDS")
    this as AccessorMatrix4f

    return org.joml.Matrix4f(
        this.a00,
        this.a01,
        this.a02,
        this.a03,
        this.a10,
        this.a11,
        this.a12,
        this.a13,
        this.a20,
        this.a21,
        this.a22,
        this.a23,
        this.a30,
        this.a31,
        this.a32,
        this.a33,
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

typealias Minecraft = MinecraftClient
typealias BlockChest = ChestBlock

typealias TileEntity = BlockEntity
typealias TileEntityBed = BedBlockEntity
typealias TileEntityChest = ChestBlockEntity
typealias TileEntityEnderChest = EnderChestBlockEntity
typealias TileEntityShulkerBox = ShulkerBoxBlockEntity

typealias EntityCow = CowEntity

typealias ResourceLocation = Identifier