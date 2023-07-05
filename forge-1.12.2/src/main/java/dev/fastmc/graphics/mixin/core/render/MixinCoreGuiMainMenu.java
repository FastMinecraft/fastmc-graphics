package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.graphics.mixin.FixedFunctionMatrixStacks;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinCoreGuiMainMenu {
    @Inject(method = "drawPanorama", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    public void drawGuiContainerBackgroundLayer(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        FixedFunctionMatrixStacks.perspective(120.0f ,1.0f, 0.05f, 10.0f);
    }
}
