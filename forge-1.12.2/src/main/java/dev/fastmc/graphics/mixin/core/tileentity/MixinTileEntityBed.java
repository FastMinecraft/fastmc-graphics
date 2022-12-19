package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.tileentity.BedInfo;
import net.minecraft.tileentity.TileEntityBed;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntityBed.class)
public abstract class MixinTileEntityBed implements BedInfo {

}