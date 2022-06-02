package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.renderbuilder.IParallelUpdate;
import me.luna.fastmc.tileentity.ChestInfo;
import net.minecraft.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBlockEntity.class)
public abstract class MixinChestBlockEntity implements ChestInfo, IParallelUpdate {

}
