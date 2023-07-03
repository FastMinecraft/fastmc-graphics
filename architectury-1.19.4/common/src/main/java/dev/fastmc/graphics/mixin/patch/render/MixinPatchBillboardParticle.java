package dev.fastmc.graphics.mixin.patch.render;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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

    @Unique
    private static final Quaternionf CACHED_QUATERNION = new Quaternionf(0.0f, 0.0f, 0.0f, 0.0f);
    @Unique
    private static final Vector3f CACHED_VEC3F = new Vector3f();

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

        Quaternionf quaternion;

        if (this.angle == 0.0f) {
            quaternion = camera.getRotation();
        } else {
            quaternion = CACHED_QUATERNION;
            quaternion.set(camera.getRotation());
            quaternion.rotateZ(MathHelper.lerp(tickDelta, this.prevAngle, this.angle));
        }

        float size = this.getSize(tickDelta);
        int brightness = this.getBrightness(tickDelta);

        float u1 = this.getMinU();
        float u2 = this.getMaxU();
        float v1 = this.getMinV();
        float v2 = this.getMaxV();

        CACHED_VEC3F.set(-1.0f, -1.0f, 0.0f);
        CACHED_VEC3F.rotate(quaternion);
        CACHED_VEC3F.mul(size);
        CACHED_VEC3F.add(x, y, z);
        vertexConsumer.vertex(CACHED_VEC3F.x, CACHED_VEC3F.y, CACHED_VEC3F.z).texture(u2, v2).color(
            this.red,
            this.green,
            this.blue,
            this.alpha
        ).light(brightness).next();

        CACHED_VEC3F.set(-1.0f, 1.0f, 0.0f);
        CACHED_VEC3F.rotate(quaternion);
        CACHED_VEC3F.mul(size);
        CACHED_VEC3F.add(x, y, z);
        vertexConsumer.vertex(CACHED_VEC3F.x, CACHED_VEC3F.y, CACHED_VEC3F.z).texture(u2, v1).color(
            this.red,
            this.green,
            this.blue,
            this.alpha
        ).light(brightness).next();

        CACHED_VEC3F.set(1.0f, 1.0f, 0.0f);
        CACHED_VEC3F.rotate(quaternion);
        CACHED_VEC3F.mul(size);
        CACHED_VEC3F.add(x, y, z);
        vertexConsumer.vertex(CACHED_VEC3F.x, CACHED_VEC3F.y, CACHED_VEC3F.z).texture(u1, v1).color(
            this.red,
            this.green,
            this.blue,
            this.alpha
        ).light(brightness).next();

        CACHED_VEC3F.set(1.0f, -1.0f, 0.0f);
        CACHED_VEC3F.rotate(quaternion);
        CACHED_VEC3F.mul(size);
        CACHED_VEC3F.add(x, y, z);
        vertexConsumer.vertex(CACHED_VEC3F.x, CACHED_VEC3F.y, CACHED_VEC3F.z).texture(u1, v2).color(
            this.red,
            this.green,
            this.blue,
            this.alpha
        ).light(brightness).next();
    }
}