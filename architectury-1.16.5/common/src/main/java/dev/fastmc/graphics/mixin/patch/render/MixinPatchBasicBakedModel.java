package dev.fastmc.graphics.mixin.patch.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Mixin(BasicBakedModel.class)
public class MixinPatchBasicBakedModel {
    @Shadow
    @Final
    protected List<BakedQuad> quads;

    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] faceQuadsArrayMap = (List<BakedQuad>[]) new List[6];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(
        List<BakedQuad> list,
        Map<Direction, List<BakedQuad>> map,
        boolean bl,
        boolean bl2,
        boolean bl3,
        Sprite sprite,
        ModelTransformation modelTransformation,
        ModelOverrideList modelOverrideList,
        CallbackInfo ci
    ) {
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            faceQuadsArrayMap[i] = map.get(directions[i]);
        }
    }

    /**
     * @author Luna
     * @reason Fast model look up
     */
    @Overwrite
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return face == null ? this.quads : this.faceQuadsArrayMap[face.ordinal()];
    }
}