package me.luna.fastmc.util

import me.luna.fastmc.mixin.accessor.AccessorMinecraft
import me.luna.fastmc.mixin.accessor.AccessorRenderGlobalContainerLocalRenderInformation
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.chunk.RenderChunk
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

val Minecraft.partialTicks: Float
    get() = if (this.isGamePaused) (this as AccessorMinecraft).renderPartialTicksPaused
    else this.renderPartialTicks

val RenderGlobal.ContainerLocalRenderInformation.renderChunk: RenderChunk
    get() = (this as AccessorRenderGlobalContainerLocalRenderInformation).renderChunk

val RenderGlobal.ContainerLocalRenderInformation.facing: EnumFacing?
    get() = (this as AccessorRenderGlobalContainerLocalRenderInformation).facing

val RenderGlobal.ContainerLocalRenderInformation.setFacing: Byte
    get() = (this as AccessorRenderGlobalContainerLocalRenderInformation).setFacing

val RenderGlobal.ContainerLocalRenderInformation.counter: Int
    get() = (this as AccessorRenderGlobalContainerLocalRenderInformation).counter

@Suppress("UNNECESSARY_SAFE_CALL")
val TileEntity.blockState: IBlockState?
    get() = this.world?.getBlockState(this.pos)

@Suppress("UNCHECKED_CAST")
fun <T : Comparable<T>> IBlockState?.getPropertyOrNull(property: IProperty<T>): T? {
    return this?.properties?.get(property) as T?
}

@Suppress("UNCHECKED_CAST")
fun <T : Comparable<T>> IBlockState?.getPropertyOrDefault(property: IProperty<T>, default: T): T {
    return this?.properties?.get(property) as? T? ?: default
}