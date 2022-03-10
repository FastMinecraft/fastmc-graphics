package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.tileentity.BedInfo;
import net.minecraft.tileentity.TileEntityBed;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntityBed.class)
public abstract class MixinTileEntityBed implements BedInfo {

}
