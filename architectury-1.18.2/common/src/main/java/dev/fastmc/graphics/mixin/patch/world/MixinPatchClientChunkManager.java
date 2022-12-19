package dev.fastmc.graphics.mixin.patch.world;

import dev.fastmc.graphics.terrain.OffThreadLightingProvider;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkManager.class)
public class MixinPatchClientChunkManager {
    @Mutable
    @Shadow
    @Final
    private LightingProvider lightingProvider;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(ClientWorld world, int loadDistance, CallbackInfo ci) {
        OffThreadLightingProvider.executor.getQueue().clear();
        this.lightingProvider = new OffThreadLightingProvider(
            (ChunkProvider) this,
            true,
            world.getDimension().hasSkyLight()
        );
    }
}