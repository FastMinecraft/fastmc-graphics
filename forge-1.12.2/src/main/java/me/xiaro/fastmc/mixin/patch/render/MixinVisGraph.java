package me.xiaro.fastmc.mixin.patch.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.xiaro.fastmc.mixin.IPatchedVisGraph;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import scala.Int;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

@Mixin(VisGraph.class)
public abstract class MixinVisGraph implements IPatchedVisGraph {
    @Shadow @Final private BitSet bitSet;

    @Shadow protected abstract void addEdges(int pos, Set<EnumFacing> p_178610_2_);
    @Shadow protected abstract int getNeighborIndexAtFace(int pos, EnumFacing facing);

    @Shadow private int empty;
    private final IntArrayList list0 = new IntArrayList();

    /**
     * @author Xiaro
     * @reason Memory allocation
     */
    @Overwrite
    private Set<EnumFacing> floodFill(int pos) {
        EnumSet<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);

        IntArrayList list = list0;
        list0.clear();
        list0.add(pos);

        this.bitSet.set(pos, true);

        while (!list.isEmpty()) {
            int i = list.removeInt(list.size() - 1);
            this.addEdges(i, set);

            for (EnumFacing enumfacing : EnumFacing.VALUES) {
                int j = this.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !this.bitSet.get(j)) {
                    this.bitSet.set(j, true);
                    list.add(j);
                }
            }
        }

        return set;
    }

    @Override
    public void setOpaqueCube(int x, int y, int z) {
        this.bitSet.set(x | y << 8 | z << 4, true);
        --this.empty;
    }
}
