package dev.fastmc.graphics.mixin.patch.render;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BillboardParticle.class)
public abstract class MixinPatchBillboardParticle extends Particle {
    protected MixinPatchBillboardParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Shadow
    public abstract float getSize(float tickDelta);

    @Shadow
    protected abstract float getMinU();

    @Shadow
    protected abstract float getMaxU();

    @Shadow
    protected abstract float getMinV();

    @Shadow
    protected abstract float getMaxV();

    private static final Quaternion QUATERNION_1 = new Quaternion(0.0f, 0.0f, 0.0f, 0.0f);
    private static final Quaternion QUATERNION_2 = new Quaternion(0.0f, 0.0f, 0.0f, 0.0f);
    private static final Vec3f VEC3F = new Vec3f();

    /**
     * @author Luna
     * @reason Particle render optimization
     */
    @SuppressWarnings("DuplicatedCode")
    @Overwrite
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3d cameraPos = camera.getPos();

        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        Quaternion quaternion;

        if (this.angle == 0.0f) {
            quaternion = camera.getRotation();
        } else {
            quaternion = QUATERNION_1;
            Quaternion other = camera.getRotation();
            quaternion.set(other.getX(), other.getY(), other.getZ(), other.getW());
            float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);
            QUATERNION_2.set(0.0f, 0.0f, MathHelper.sin(angle / 2.0f), MathHelper.cos(angle / 2.0f));
            quaternion.hamiltonProduct(QUATERNION_2);
        }

        float size = this.getSize(tickDelta);
        int brightness = this.getBrightness(tickDelta);

        float u1 = this.getMinU();
        float u2 = this.getMaxU();
        float v1 = this.getMinV();
        float v2 = this.getMaxV();

        VEC3F.set(-1.0f, -1.0f, 0.0f);
        VEC3F.rotate(quaternion);
        VEC3F.scale(size);
        VEC3F.add(x, y, z);
        vertexConsumer.vertex(VEC3F.getX(), VEC3F.getY(), VEC3F.getZ()).texture(u2, v2).color(
            this.colorRed,
            this.colorGreen,
            this.colorBlue,
            this.colorAlpha
        ).light(brightness).next();

        VEC3F.set(-1.0f, 1.0f, 0.0f);
        VEC3F.rotate(quaternion);
        VEC3F.scale(size);
        VEC3F.add(x, y, z);
        vertexConsumer.vertex(VEC3F.getX(), VEC3F.getY(), VEC3F.getZ()).texture(u2, v1).color(
            this.colorRed,
            this.colorGreen,
            this.colorBlue,
            this.colorAlpha
        ).light(brightness).next();

        VEC3F.set(1.0f, 1.0f, 0.0f);
        VEC3F.rotate(quaternion);
        VEC3F.scale(size);
        VEC3F.add(x, y, z);
        vertexConsumer.vertex(VEC3F.getX(), VEC3F.getY(), VEC3F.getZ()).texture(u1, v1).color(
            this.colorRed,
            this.colorGreen,
            this.colorBlue,
            this.colorAlpha
        ).light(brightness).next();

        VEC3F.set(1.0f, -1.0f, 0.0f);
        VEC3F.rotate(quaternion);
        VEC3F.scale(size);
        VEC3F.add(x, y, z);
        vertexConsumer.vertex(VEC3F.getX(), VEC3F.getY(), VEC3F.getZ()).texture(u1, v2).color(
            this.colorRed,
            this.colorGreen,
            this.colorBlue,
            this.colorAlpha
        ).light(brightness).next();
    }
}