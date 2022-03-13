package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.tileentity.TileEntityInfo;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntity.class)
public class MixinBlockEntity implements TileEntityInfo<BlockEntity> {
    private final int typeID = TileEntityInfo.super.getTypeID();

    @Override
    @NotNull
    public BlockEntity getEntity() {
        return (BlockEntity) ((Object) this);
    }

    @Override
    public int getTypeID() {
        return typeID;
    }
}
