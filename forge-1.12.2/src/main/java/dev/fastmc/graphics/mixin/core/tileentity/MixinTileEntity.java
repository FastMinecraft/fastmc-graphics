package dev.fastmc.graphics.mixin.core.tileentity;

import dev.fastmc.graphics.tileentity.TileEntityInfo;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntity.class)
public class MixinTileEntity implements TileEntityInfo<TileEntity> {
    private final int typeID = TileEntityInfo.super.getTypeID();

    @Override
    @NotNull
    public TileEntity getEntity() {
        return (TileEntity) ((Object) this);
    }

    @Override
    public int getTypeID() {
        return typeID;
    }
}