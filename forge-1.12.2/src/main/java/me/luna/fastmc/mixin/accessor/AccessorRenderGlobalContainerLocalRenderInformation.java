package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(RenderGlobal.ContainerLocalRenderInformation.class)
public interface AccessorRenderGlobalContainerLocalRenderInformation {
    @Accessor("renderChunk")
    RenderChunk getRenderChunk();

    @Accessor("facing")
    EnumFacing getFacing();

    @Accessor("setFacing")
    byte getSetFacing();

    @Accessor("counter")
    int getCounter();
}
