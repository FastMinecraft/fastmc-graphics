package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.instancing.IParallelUpdate;
import me.luna.fastmc.tileentity.EnderChestInfo;
import net.minecraft.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnderChestBlockEntity.class)
public abstract class MixinEnderChestBlockEntity implements EnderChestInfo, IParallelUpdate {

}
