package dev.fastmc.graphics.mixin.patch.render;

import dev.fastmc.graphics.FastMcMod;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClippingHelperImpl.class)
public abstract class MixinClippingHelperImpl extends ClippingHelper {
    @Shadow
    protected abstract void normalize(float[] p_180547_1_);

    /**
     * @author Luna
     * @reason OpenGL synchronization optimization
     */
    @Overwrite
    public void init() {
        float[] array1 = this.projectionMatrix;
        float[] array2 = this.modelviewMatrix;

        FastMcMod.INSTANCE.getWorldRenderer().getProjectionMatrix().get(array1);
        FastMcMod.INSTANCE.getWorldRenderer().getModelViewMatrix().get(array2);

        this.clippingMatrix[0] = array2[0] * array1[0] + array2[1] * array1[4] + array2[2] * array1[8] + array2[3] * array1[12];
        this.clippingMatrix[1] = array2[0] * array1[1] + array2[1] * array1[5] + array2[2] * array1[9] + array2[3] * array1[13];
        this.clippingMatrix[2] = array2[0] * array1[2] + array2[1] * array1[6] + array2[2] * array1[10] + array2[3] * array1[14];
        this.clippingMatrix[3] = array2[0] * array1[3] + array2[1] * array1[7] + array2[2] * array1[11] + array2[3] * array1[15];
        this.clippingMatrix[4] = array2[4] * array1[0] + array2[5] * array1[4] + array2[6] * array1[8] + array2[7] * array1[12];
        this.clippingMatrix[5] = array2[4] * array1[1] + array2[5] * array1[5] + array2[6] * array1[9] + array2[7] * array1[13];
        this.clippingMatrix[6] = array2[4] * array1[2] + array2[5] * array1[6] + array2[6] * array1[10] + array2[7] * array1[14];
        this.clippingMatrix[7] = array2[4] * array1[3] + array2[5] * array1[7] + array2[6] * array1[11] + array2[7] * array1[15];
        this.clippingMatrix[8] = array2[8] * array1[0] + array2[9] * array1[4] + array2[10] * array1[8] + array2[11] * array1[12];
        this.clippingMatrix[9] = array2[8] * array1[1] + array2[9] * array1[5] + array2[10] * array1[9] + array2[11] * array1[13];
        this.clippingMatrix[10] = array2[8] * array1[2] + array2[9] * array1[6] + array2[10] * array1[10] + array2[11] * array1[14];
        this.clippingMatrix[11] = array2[8] * array1[3] + array2[9] * array1[7] + array2[10] * array1[11] + array2[11] * array1[15];
        this.clippingMatrix[12] = array2[12] * array1[0] + array2[13] * array1[4] + array2[14] * array1[8] + array2[15] * array1[12];
        this.clippingMatrix[13] = array2[12] * array1[1] + array2[13] * array1[5] + array2[14] * array1[9] + array2[15] * array1[13];
        this.clippingMatrix[14] = array2[12] * array1[2] + array2[13] * array1[6] + array2[14] * array1[10] + array2[15] * array1[14];
        this.clippingMatrix[15] = array2[12] * array1[3] + array2[13] * array1[7] + array2[14] * array1[11] + array2[15] * array1[15];

        float[] afloat2 = this.frustum[0];
        afloat2[0] = this.clippingMatrix[3] - this.clippingMatrix[0];
        afloat2[1] = this.clippingMatrix[7] - this.clippingMatrix[4];
        afloat2[2] = this.clippingMatrix[11] - this.clippingMatrix[8];
        afloat2[3] = this.clippingMatrix[15] - this.clippingMatrix[12];
        this.normalize(afloat2);

        float[] afloat3 = this.frustum[1];
        afloat3[0] = this.clippingMatrix[3] + this.clippingMatrix[0];
        afloat3[1] = this.clippingMatrix[7] + this.clippingMatrix[4];
        afloat3[2] = this.clippingMatrix[11] + this.clippingMatrix[8];
        afloat3[3] = this.clippingMatrix[15] + this.clippingMatrix[12];
        this.normalize(afloat3);

        float[] afloat4 = this.frustum[2];
        afloat4[0] = this.clippingMatrix[3] + this.clippingMatrix[1];
        afloat4[1] = this.clippingMatrix[7] + this.clippingMatrix[5];
        afloat4[2] = this.clippingMatrix[11] + this.clippingMatrix[9];
        afloat4[3] = this.clippingMatrix[15] + this.clippingMatrix[13];
        this.normalize(afloat4);

        float[] afloat5 = this.frustum[3];
        afloat5[0] = this.clippingMatrix[3] - this.clippingMatrix[1];
        afloat5[1] = this.clippingMatrix[7] - this.clippingMatrix[5];
        afloat5[2] = this.clippingMatrix[11] - this.clippingMatrix[9];
        afloat5[3] = this.clippingMatrix[15] - this.clippingMatrix[13];
        this.normalize(afloat5);

        float[] afloat6 = this.frustum[4];
        afloat6[0] = this.clippingMatrix[3] - this.clippingMatrix[2];
        afloat6[1] = this.clippingMatrix[7] - this.clippingMatrix[6];
        afloat6[2] = this.clippingMatrix[11] - this.clippingMatrix[10];
        afloat6[3] = this.clippingMatrix[15] - this.clippingMatrix[14];
        this.normalize(afloat6);

        float[] afloat7 = this.frustum[5];
        afloat7[0] = this.clippingMatrix[3] + this.clippingMatrix[2];
        afloat7[1] = this.clippingMatrix[7] + this.clippingMatrix[6];
        afloat7[2] = this.clippingMatrix[11] + this.clippingMatrix[10];
        afloat7[3] = this.clippingMatrix[15] + this.clippingMatrix[14];
        this.normalize(afloat7);
    }
}