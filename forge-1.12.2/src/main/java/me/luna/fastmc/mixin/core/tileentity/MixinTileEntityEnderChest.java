package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.tileentity.ChestInfo;
import me.luna.fastmc.tileentity.EnderChestInfo;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntityEnderChest.class)
public abstract class MixinTileEntityEnderChest implements EnderChestInfo {

}
