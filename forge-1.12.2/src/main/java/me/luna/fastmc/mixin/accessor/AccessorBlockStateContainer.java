package me.luna.fastmc.mixin.accessor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.BlockStateContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockStateContainer.class)
public interface AccessorBlockStateContainer {
    @Invoker("get")
    IBlockState invokeGet(int index);
}
