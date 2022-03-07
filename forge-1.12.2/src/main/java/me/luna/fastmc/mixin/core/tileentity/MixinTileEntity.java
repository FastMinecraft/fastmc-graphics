package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.tileentity.TileEntityInfo;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "shouldRenderInPass", at = @At("HEAD"), cancellable = true, remap = false)
    public void renderEntities$Inject$INVOKE$release(int pass, CallbackInfoReturnable<Boolean> cir) {
        if (((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer()).hasRenderer(getEntity())) {
            cir.setReturnValue(false);
        }
    }
}
