package dev.fastmc.graphics.mixin.patch.render;

import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.FixedFunctionMatrixStacks;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ActiveRenderInfo.class)
public class MixinActiveRenderInfo {
    @Shadow
    private static float rotationX;
    @Shadow
    private static float rotationZ;
    @Shadow
    private static float rotationYZ;
    @Shadow
    private static float rotationXY;
    @Shadow
    private static float rotationXZ;
    @Shadow
    private static Vec3d position;

    private static final Vector4f TEMP_VECTOR = new Vector4f();

    /**
     * @author Luna
     * @reason OpenGL synchronization optimization
     */
    @Overwrite(remap = false)
    public static void updateRenderInfo(Entity entityplayerIn, boolean p_74583_1_) {
        Matrix4fStack project = FixedFunctionMatrixStacks.PROJECTION;
        project.pushMatrix();
        project.invert();

        Matrix4fStack modelView = FixedFunctionMatrixStacks.MODELVIEW;
        modelView.pushMatrix();
        modelView.invert();
        modelView.mul(project, modelView);

        TEMP_VECTOR.set(0.0f, 0.0f, 0.0f, 1.0f);
        TEMP_VECTOR.mulProject(modelView);

        project.popMatrix();
        modelView.popMatrix();

        position = new Vec3d(TEMP_VECTOR.x, TEMP_VECTOR.y, TEMP_VECTOR.z);

        int i = p_74583_1_ ? 1 : 0;
        float f2 = entityplayerIn.rotationPitch;
        float f3 = entityplayerIn.rotationYaw;

        rotationX = MathHelper.cos(f3 * 0.017453292F) * (float) (1 - i * 2);
        rotationZ = MathHelper.sin(f3 * 0.017453292F) * (float) (1 - i * 2);
        rotationYZ = -rotationZ * MathHelper.sin(f2 * 0.017453292F) * (float) (1 - i * 2);
        rotationXY = rotationX * MathHelper.sin(f2 * 0.017453292F) * (float) (1 - i * 2);
        rotationXZ = MathHelper.cos(f2 * 0.017453292F);
    }
}