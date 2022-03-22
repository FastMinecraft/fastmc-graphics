package me.luna.fastmc.mixin.patch.render;

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

            double distance = calcSquaredDistance0(
                seed,
                x2 + xBit,
                y2 + yBit,
                z2 + zBit,
                x3 - (double) xBit,
                y3 - (double) yBit,
                z3 - (double) zBit
            );

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

    private static double calcSquaredDistance0(long seed, int x, int y, int z, double xFraction, double yFraction, double zFraction) {
        long l = SeedMixer.mixSeed(seed, x);
        l = SeedMixer.mixSeed(l, y);
        l = SeedMixer.mixSeed(l, z);
        l = SeedMixer.mixSeed(l, x);
        l = SeedMixer.mixSeed(l, y);
        l = SeedMixer.mixSeed(l, z);

        long temp = l >> 24;
        double x2 = xFraction + ((double) (int) (temp & 1023) / 1024.0D - 0.5D) * 0.9D;

        l = SeedMixer.mixSeed(l, seed);

        temp = l >> 24;
        double y2 = yFraction + ((double) (int) (temp & 1023) / 1024.0D - 0.5D) * 0.9D;

        l = SeedMixer.mixSeed(l, seed);

        temp = l >> 24;
        double z2 = zFraction + ((double) (int) (temp & 1023) / 1024.0D - 0.5D) * 0.9D;

        return z2 * z2 + y2 * y2 + x2 * x2;
    }
}
