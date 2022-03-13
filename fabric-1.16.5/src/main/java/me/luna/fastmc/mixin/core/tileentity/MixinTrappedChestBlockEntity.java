package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.renderbuilder.IParallelUpdate;
import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo;
import me.luna.fastmc.tileentity.ChestInfo;
import me.luna.fastmc.tileentity.TileEntityInfo;
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
