package dev.fastmc.graphics.mixin.patch.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WeightedBakedModel.class)
public class MixinWeightedBakedModel {
    @Shadow
    @Final
    private int totalWeight;

    private BakedModel[] modelArray;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(List<Weighted.Present<BakedModel>> models, CallbackInfo ci) {
        modelArray = new BakedModel[totalWeight];

        int weight = 0;
        for (int i = 0; i < models.size(); i++) {
            Weighted.Present<BakedModel> entry = models.get(i);
            int start = weight;
            weight += entry.getWeight().getValue();
            for (int j = start; j < weight; j++) {
                modelArray[j] = entry.getData();
            }
        }
    }

    /**
     * @author Luna
     * @reason Fast model look up
     */
    @Overwrite
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return modelArray[(random.nextInt() & 0x7FFFFFFF) % this.totalWeight].getQuads(state, face, random);
    }

}