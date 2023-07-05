package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.graphics.mixin.FixedFunctionMatrixStacks;
import net.minecraft.client.gui.GuiEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEnchantment.class)
public class MixinCoreGuiEnchantment {
    @Inject(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    public void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        FixedFunctionMatrixStacks.perspective(90.0f, 1.3333334f, 9.0f, 80.0f);
    }
}
