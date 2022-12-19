package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.shared.instancing.IParallelUpdate;
import dev.fastmc.graphics.tileentity.EnderChestInfo;
import net.minecraft.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnderChestBlockEntity.class)
public abstract class MixinEnderChestBlockEntity implements EnderChestInfo, IParallelUpdate {

}