package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.shared.renderer.WorldRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
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

    private static final Matrix4f COMBINED_MATRIX = new Matrix4f();
    private static final Vector4f VECTOR = new Vector4f();

    /**
     * @author Luna
     * @reason OpenGL synchronization optimization
     */
    @Overwrite(remap = false)
    public static void updateRenderInfo(Entity entityplayerIn, boolean p_74583_1_) {
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        Matrix4f invertedModelViewMatrix = worldRenderer.getInverseModelViewMatrix();
        Matrix4f invertedProjectMatrix = worldRenderer.getInverseProjectMatrix();
        invertedModelViewMatrix.mul(invertedProjectMatrix, COMBINED_MATRIX);

        VECTOR.set(0.0f, 0.0f, 0.0f, 1.0f);
        VECTOR.mulProject(COMBINED_MATRIX);
        position = new Vec3d(VECTOR.x, VECTOR.y, VECTOR.z);

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
