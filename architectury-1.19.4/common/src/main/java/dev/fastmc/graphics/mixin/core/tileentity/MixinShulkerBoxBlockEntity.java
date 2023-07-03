package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.shared.instancing.IParallelUpdate;
import dev.fastmc.graphics.tileentity.ShulkerBoxInfo;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class MixinShulkerBoxBlockEntity implements ShulkerBoxInfo, IParallelUpdate {

}