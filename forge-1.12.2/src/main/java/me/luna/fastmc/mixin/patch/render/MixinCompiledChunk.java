package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CompiledChunk.class)
public class MixinCompiledChunk {
    @Mutable
    @Shadow
    @Final
    private List<TileEntity> tileEntities;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(CallbackInfo ci) {
        this.tileEntities = new FastObjectArrayList<>();
    }
}
