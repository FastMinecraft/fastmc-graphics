package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.shared.instancing.IParallelUpdate;
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo;
import dev.fastmc.graphics.tileentity.ChestInfo;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TrappedChestBlockEntity.class)
public abstract class MixinTrappedChestBlockEntity implements ChestInfo, IParallelUpdate {
    private final int typeID = ITileEntityInfo.Companion.getRegistry().get(ChestBlockEntity.class);

    @Override
    public int getTypeID() {
        return typeID;
    }
}