package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.renderbuilder.IParallelUpdate;
import me.luna.fastmc.tileentity.ShulkerBoxInfo;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class MixinShulkerBoxBlockEntity implements ShulkerBoxInfo, IParallelUpdate {

}
