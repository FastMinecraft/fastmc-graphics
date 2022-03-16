package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedMatrix3f;
import me.luna.fastmc.mixin.IPatchedMatrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("DuplicatedCode")
@Mixin(Matrix3f.class)
public class MixinPatchMatrix3f implements IPatchedMatrix3f {
    @Shadow
    public float a00;
    @Shadow
    public float a01;
    @Shadow
    public float a02;
    @Shadow
    public float a10;
    @Shadow
    public float a11;
    @Shadow
    public float a12;
    @Shadow
    public float a20;
    @Shadow
    public float a21;
    @Shadow
    public float a22;

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

        float na00 = Math.fma(a00, b00, Math.fma(a01, b10, a02 * b20));
        float na10 = Math.fma(a10, b00, Math.fma(a11, b10, a12 * b20));
        float na20 = Math.fma(a20, b00, Math.fma(a21, b10, a22 * b20));

        float na01 = Math.fma(a00, b01, Math.fma(a01, b11, a02 * b21));
        float na11 = Math.fma(a10, b01, Math.fma(a11, b11, a12 * b21));
        float na21 = Math.fma(a20, b01, Math.fma(a21, b11, a22 * b21));

        float na02 = Math.fma(a00, b02, Math.fma(a01, b12, a02 * b22));
        float na12 = Math.fma(a10, b02, Math.fma(a11, b12, a12 * b22));
        float na22 = Math.fma(a20, b02, Math.fma(a21, b12, a22 * b22));

        this.a00 = na00;
        this.a10 = na10;
        this.a20 = na20;

        this.a01 = na01;
        this.a11 = na11;
        this.a21 = na21;

        this.a02 = na02;
        this.a12 = na12;
        this.a22 = na22;
    }

    @Override
    public void scale(float x, float y, float z) {
        a00 = a00 * x;
        a10 = a10 * x;
        a20 = a20 * x;
        a01 = a01 * y;
        a11 = a11 * y;
        a21 = a21 * y;
        a02 = a02 * z;
        a12 = a12 * z;
        a22 = a22 * z;
    }

    @Override
    public void set(@NotNull Matrix3f other) {
        this.a00 = other.a00;
        this.a10 = other.a10;
        this.a20 = other.a20;

        this.a01 = other.a01;
        this.a11 = other.a11;
        this.a21 = other.a21;

        this.a02 = other.a02;
        this.a12 = other.a12;
        this.a22 = other.a22;
    }
}
