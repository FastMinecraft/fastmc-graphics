package me.luna.fastmc.mixin.patch.world;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.biome.source.VoronoiBiomeAccessType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VoronoiBiomeAccessType.class)
public abstract class MixinPatchVoronoiBiomeAccessType {
    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public Biome getBiome(long seed, int x, int y, int z, BiomeAccess.Storage storage) {
        int x1 = x - 2;
        int y1 = y - 2;
        int z1 = z - 2;

        int x2 = x1 >> 2;
        int y2 = y1 >> 2;
        int z2 = z1 >> 2;

        double x3 = (double) (x1 & 3) / 4.0D;
        double y3 = (double) (y1 & 3) / 4.0D;
        double z3 = (double) (z1 & 3) / 4.0D;

        int minIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < 8; ++i) {
            int xBit = (i & 4) >> 2 & 1;
            int yBit = (i & 2) >> 1 & 1;
            int zBit = i & 1;

            long l = SeedMixer.mixSeed(seed, x2 + xBit);
            l = SeedMixer.mixSeed(l, y2 + yBit);
            l = SeedMixer.mixSeed(l, z2 + zBit);
            l = SeedMixer.mixSeed(l, x2 + xBit);
            l = SeedMixer.mixSeed(l, y2 + yBit);
            l = SeedMixer.mixSeed(l, z2 + zBit);

            long temp = l >> 24;
            double x21 = x3 - (double) xBit + ((double) (int) (temp & 1023) / 1024.0D - 0.5D) * 0.9D;

            l = SeedMixer.mixSeed(l, seed);

            temp = l >> 24;
            double y21 = y3 - (double) yBit + ((double) (int) (temp & 1023) / 1024.0D - 0.5D) * 0.9D;

            l = SeedMixer.mixSeed(l, seed);

            temp = l >> 24;
            double z21 = z3 - (double) zBit + ((double) (int) (temp & 1023) / 1024.0D - 0.5D) * 0.9D;

            double distance = z21 * z21 + y21 * y21 + x21 * x21;

            if (distance < minDistance) {
                minIndex = i;
                minDistance = distance;
            }
        }

        int x4 = (minIndex & 4) == 0 ? x2 : x2 + 1;
        int y4 = (minIndex & 2) == 0 ? y2 : y2 + 1;
        int z4 = (minIndex & 1) == 0 ? z2 : z2 + 1;
        return storage.getBiomeForNoiseGen(x4, y4, z4);
    }

}
