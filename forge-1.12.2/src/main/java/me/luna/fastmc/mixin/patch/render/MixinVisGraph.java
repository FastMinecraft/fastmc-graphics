package me.luna.fastmc.mixin.patch.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.luna.fastmc.mixin.IPatchedVisGraph;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

@Mixin(VisGraph.class)
public abstract class MixinVisGraph implements IPatchedVisGraph {
    @Shadow @Final private BitSet bitSet;

    @Shadow protected abstract void addEdges(int pos, Set<EnumFacing> p_178610_2_);
    @Shadow protected abstract int getNeighborIndexAtFace(int pos, EnumFacing facing);

    @Shadow private int empty;

    /**
     * @author Luna
     * @reason Memory allocation
     */
    @Overwrite
    private Set<EnumFacing> floodFill(int pos) {
        EnumSet<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);

        this.bitSet.set(pos, true);
        recursiveFloodFill(set, pos);

        return set;
    }

    private void recursiveFloodFill(EnumSet<EnumFacing> set, int pos) {
        this.addEdges(pos, set);

        for (EnumFacing enumfacing : EnumFacing.VALUES) {
            int j = this.getNeighborIndexAtFace(pos, enumfacing);

            if (j >= 0 && !this.bitSet.get(j)) {
                this.bitSet.set(j, true);
                recursiveFloodFill(set, j);
            }
        }
    }

    @Override
    public void setOpaqueCube(int x, int y, int z) {
        this.bitSet.set(x | y << 8 | z << 4, true);
        --this.empty;
    }
}
