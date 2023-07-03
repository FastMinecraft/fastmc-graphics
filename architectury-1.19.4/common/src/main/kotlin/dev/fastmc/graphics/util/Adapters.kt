@file:Suppress("NOTHING_TO_INLINE")

package dev.fastmc.graphics.util

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.*
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.CowEntity
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler

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