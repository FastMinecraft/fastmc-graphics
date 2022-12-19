package dev.fastmc.graphics.mixin.patch.render;

import dev.fastmc.graphics.mixin.IPatchedMatrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public class MixinPatchMatrix4f implements IPatchedMatrix4f {
    @Shadow
    public float a00;
    @Shadow
    public float a01;
    @Shadow
    public float a02;
    @Shadow
    public float a03;
    @Shadow
    public float a10;
    @Shadow
    public float a11;
    @Shadow
    public float a12;
    @Shadow
    public float a13;
    @Shadow
    public float a20;
    @Shadow
    public float a21;
    @Shadow
    public float a22;
    @Shadow
    public float a23;
    @Shadow
    public float a30;
    @Shadow
    public float a31;
    @Shadow
    public float a32;
    @Shadow
    public float a33;

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Environment(EnvType.CLIENT)
    @Overwrite
    public void multiply(Quaternion quaternion) {
        float x = quaternion.getX();
        float y = quaternion.getY();
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float j = 2.0F * x * x;
        float k = 2.0F * y * y;
        float l = 2.0F * z * z;

        float b00 = 1.0F - k - l;
        float b11 = 1.0F - l - j;
        float b22 = 1.0F - j - k;

        float m = x * y;
        float n = y * z;
        float o = z * x;
        float p = x * w;
        float q = y * w;
        float r = z * w;

        float b10 = 2.0F * (m + r);
        float b01 = 2.0F * (m - r);
        float b20 = 2.0F * (o - q);
        float b02 = 2.0F * (o + q);
        float b21 = 2.0F * (n + p);
        float b12 = 2.0F * (n - p);

        float t00 = this.a00 * b00 + this.a01 * b10 + this.a02 * b20;
        float t01 = this.a00 * b01 + this.a01 * b11 + this.a02 * b21;
        float t02 = this.a00 * b02 + this.a01 * b12 + this.a02 * b22;

        float t10 = this.a10 * b00 + this.a11 * b10 + this.a12 * b20;
        float t11 = this.a10 * b01 + this.a11 * b11 + this.a12 * b21;
        float t12 = this.a10 * b02 + this.a11 * b12 + this.a12 * b22;

        float t20 = this.a20 * b00 + this.a21 * b10 + this.a22 * b20;
        float t21 = this.a20 * b01 + this.a21 * b11 + this.a22 * b21;
        float t22 = this.a20 * b02 + this.a21 * b12 + this.a22 * b22;

        float t30 = this.a30 * b00 + this.a31 * b10 + this.a32 * b20;
        float t31 = this.a30 * b01 + this.a31 * b11 + this.a32 * b21;
        float t32 = this.a30 * b02 + this.a31 * b12 + this.a32 * b22;

        this.a00 = t00;
        this.a01 = t01;
        this.a02 = t02;

        this.a10 = t10;
        this.a11 = t11;
        this.a12 = t12;

        this.a20 = t20;
        this.a21 = t21;
        this.a22 = t22;

        this.a30 = t30;
        this.a31 = t31;
        this.a32 = t32;
    }

    @Override
    public void translate(float x, float y, float z) {
        a03 = Math.fma(a00, x, Math.fma(a01, y, Math.fma(a02, z, a03)));
        a13 = Math.fma(a10, x, Math.fma(a11, y, Math.fma(a12, z, a13)));
        a23 = Math.fma(a20, x, Math.fma(a21, y, Math.fma(a22, z, a23)));
        a33 = Math.fma(a30, x, Math.fma(a31, y, Math.fma(a32, z, a33)));
    }

    @Override
    public void scale(float x, float y, float z) {
        a00 = a00 * x;
        a10 = a10 * x;
        a20 = a20 * x;
        a30 = a30 * x;

        a01 = a01 * y;
        a11 = a11 * y;
        a21 = a21 * y;
        a31 = a31 * y;

        a02 = a02 * z;
        a12 = a12 * z;
        a22 = a22 * z;
        a32 = a32 * z;
    }

    @Override
    public void set(@NotNull Matrix4f other) {
        this.a00 = other.a00;
        this.a10 = other.a10;
        this.a20 = other.a20;
        this.a30 = other.a30;

        this.a01 = other.a01;
        this.a11 = other.a11;
        this.a21 = other.a21;
        this.a31 = other.a31;

        this.a02 = other.a02;
        this.a12 = other.a12;
        this.a22 = other.a22;
        this.a32 = other.a32;

        this.a03 = other.a03;
        this.a13 = other.a13;
        this.a23 = other.a23;
        this.a33 = other.a33;
    }
}