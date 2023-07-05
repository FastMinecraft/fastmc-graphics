package dev.fastmc.graphics.mixin;

import dev.fastmc.common.collection.FastObjectArrayList;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.accessor.AccessorContainerLocalRenderInformation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

public class DummyCompiledChunk extends CompiledChunk {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<TileEntity> getTileEntities() {
        return (FastObjectArrayList<TileEntity>) (Object) FastMcMod.INSTANCE.getWorldRenderer()
            .getTerrainRenderer()
            .getRenderTileEntityList()
            .getFront();
    }

    public static List<RenderGlobal.ContainerLocalRenderInformation> makeDummyInfo(RenderGlobal renderGlobal) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            RenderGlobal.ContainerLocalRenderInformation dummyInfo = (RenderGlobal.ContainerLocalRenderInformation) unsafe.allocateInstance(
                RenderGlobal.ContainerLocalRenderInformation.class);
            AccessorContainerLocalRenderInformation accessor = (AccessorContainerLocalRenderInformation) dummyInfo;


            //noinspection DataFlowIssue
            RenderChunk dummyChunk = new RenderChunk(null, renderGlobal, 0);
            accessor.setRenderChunk(dummyChunk);
            dummyChunk.compiledChunk = new DummyCompiledChunk();

            FastObjectArrayList<RenderGlobal.ContainerLocalRenderInformation> list = new FastObjectArrayList<>(1);
            list.add(dummyInfo);
            return new ReadOnlyList<>(list);
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
