package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.shared.instancing.IParallelUpdate;
import dev.fastmc.graphics.tileentity.ChestInfo;
import net.minecraft.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBlockEntity.class)
public abstract class MixinChestBlockEntity implements ChestInfo, IParallelUpdate {

}