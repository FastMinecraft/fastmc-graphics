package me.luna.fastmc.mixin.patch.world;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.renderer.WorldRenderer;
import me.luna.fastmc.util.OffThreadLightingProvider;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.*;
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
        this.lightingProvider = new OffThreadLightingProvider((ChunkProvider) this, true, world.getDimension().hasSkyLight());
    }

    /**
     * @author Luna
     * @reason Off thread light update fix
     */
    @Overwrite
    public void onLightUpdate(LightType type, ChunkSectionPos pos) {
        ((WorldRenderer) FastMcMod.INSTANCE.getWorldRenderer()).scheduleLightUpdate(type, pos);
    }
}
