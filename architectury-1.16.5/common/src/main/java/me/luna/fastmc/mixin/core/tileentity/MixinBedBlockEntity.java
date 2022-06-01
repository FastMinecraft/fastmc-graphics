package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.tileentity.BedInfo;
import net.minecraft.block.entity.BedBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BedBlockEntity.class)
public abstract class MixinBedBlockEntity implements BedInfo {

}
