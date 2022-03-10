package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RenderGlobal.class)
public interface AccessorRenderGlobal {
    @Accessor("renderInfos")
    List<RenderGlobal.ContainerLocalRenderInformation> getRenderInfos();
}
