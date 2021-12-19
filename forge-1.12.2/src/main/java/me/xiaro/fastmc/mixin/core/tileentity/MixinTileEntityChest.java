package me.xiaro.fastmc.mixin.core.tileentity;

import me.xiaro.fastmc.tileentity.ChestInfo;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest implements ChestInfo {

}
