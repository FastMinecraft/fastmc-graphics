package me.xiaro.fastmc.mixin.core.tileentity;

import me.xiaro.fastmc.tileentity.ChestInfo;
import me.xiaro.fastmc.tileentity.ShulkerBoxInfo;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntityShulkerBox.class)
public abstract class MixinTileEntityShulkerBox implements ShulkerBoxInfo {

}
