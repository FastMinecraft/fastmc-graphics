package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.tileentity.BedInfo;
import net.minecraft.block.entity.BedBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BedBlockEntity.class)
public abstract class MixinBedBlockEntity implements BedInfo {

}