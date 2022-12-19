package dev.fastmc.graphics.mixin.patch.render;

import net.minecraft.client.render.model.WeightedBakedModel;
import org.spongepowered.asm.mixin.Mixin;

// Broken in forge
@Mixin(WeightedBakedModel.class)
public class MixinWeightedBakedModel {
//    @Shadow
//    @Final
//    private int totalWeight;
//
//    private WeightedBakedModel.Entry[] modelArray;
//
//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void init$Inject$RETURN(List<WeightedBakedModel.Entry> models, CallbackInfo ci) {
//        modelArray = new WeightedBakedModel.Entry[totalWeight];
//
//        int weight = 0;
//        for (int i = 0; i < models.size(); i++) {
//            WeightedBakedModel.Entry entry = models.get(i);
//            int start = weight;
//            weight += entry.weight;
//            for (int j = start; j < weight; j++) {
//                modelArray[j] = entry;
//            }
//        }
//    }
//
//    /**
//     * @author Luna
//     * @reason Fast model look up
//     */
//    @Overwrite
//    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
//        return modelArray[(random.nextInt() & 0x7FFFFFFF) % this.totalWeight].model.getQuads(state, face, random);
//    }
//
}