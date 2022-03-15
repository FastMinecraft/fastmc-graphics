package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.FastMcMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(value = ActiveRenderInfo.class, remap = false)
public class MixinActiveRenderInfo {
    private final static Minecraft mc = Minecraft.getMinecraft();
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
    @Shadow
    @Final
    private static FloatBuffer MODELVIEW;
    @Shadow
    @Final
    private static IntBuffer VIEWPORT;
    @Shadow
    @Final
    private static FloatBuffer OBJECTCOORDS;
    @Shadow
    @Final
    private static FloatBuffer PROJECTION;

    /**
     * @author Luna
     * @reason OpenGL synchronization optimization
     */
    @Overwrite
    public static void updateRenderInfo(Entity entityplayerIn, boolean p_74583_1_) {
        Matrix4f projection = FastMcMod.INSTANCE.getWorldRenderer().getProjectionMatrix();
        Matrix4f modelView = FastMcMod.INSTANCE.getWorldRenderer().getModelViewMatrix();
        Matrix4f finalMatrix = modelView.mul(projection, new Matrix4f()).invert();

        Vector4f objectCoords = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

        objectCoords.mul(finalMatrix);
        if (objectCoords.w != 0.0f) objectCoords.normalize();

        position = new Vec3d(objectCoords.x, objectCoords.y, objectCoords.z);

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
