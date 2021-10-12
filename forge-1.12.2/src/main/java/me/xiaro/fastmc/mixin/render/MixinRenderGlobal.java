package me.xiaro.fastmc.mixin.render;

import me.xiaro.fastmc.AbstractEntityRenderer;
import me.xiaro.fastmc.EntityRenderer;
import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.resource.IResourceManager;
import me.xiaro.fastmc.resource.ResourceManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Minecraft mc;
    @Shadow private WorldClient world;
    @Shadow @Final private Map<Integer, DestroyBlockProgress> damagedBlocks;

    @Shadow protected abstract void preRenderDamagedBlocks();
    @Shadow protected abstract void postRenderDamagedBlocks();

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 3, shift = At.Shift.AFTER), cancellable = true)
    public void renderEntities$INVOKE$endStartSection$3$AFTER(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        ci.cancel();

        RenderHelper.enableStandardItemLighting();
        AbstractEntityRenderer entityRenderer = FastMcMod.INSTANCE.getEntityRenderer();

        entityRenderer.preRender();
        entityRenderer.renderTileEntities();

        this.preRenderDamagedBlocks();

        for (DestroyBlockProgress destroyblockprogress : this.damagedBlocks.values()) {
            BlockPos blockpos = destroyblockprogress.getPosition();
            IBlockState blockState = this.world.getBlockState(blockpos);

            if (blockState.getBlock().hasTileEntity(blockState)) {
                TileEntity tileentity = this.world.getTileEntity(blockpos);

                if (tileentity instanceof TileEntityChest) {
                    TileEntityChest tileentitychest = (TileEntityChest) tileentity;

                    if (tileentitychest.adjacentChestXNeg != null) {
                        blockpos = blockpos.offset(EnumFacing.WEST);
                        tileentity = this.world.getTileEntity(blockpos);
                        blockState = this.world.getBlockState(blockpos);
                    } else if (tileentitychest.adjacentChestZNeg != null) {
                        blockpos = blockpos.offset(EnumFacing.NORTH);
                        tileentity = this.world.getTileEntity(blockpos);
                        blockState = this.world.getBlockState(blockpos);
                    }
                }

                if (tileentity != null && blockState.hasCustomBreakingProgress()) {
                    TileEntityRendererDispatcher.instance.render(tileentity, partialTicks, destroyblockprogress.getPartialBlockDamage());
                }
            }
        }

        this.postRenderDamagedBlocks();
        this.mc.entityRenderer.disableLightmap();
        this.mc.profiler.endSection();

        entityRenderer.postRender();
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = this.mc;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractEntityRenderer entityRenderer = new EntityRenderer(mc, resourceManager);

        FastMcMod.INSTANCE.reloadResource(resourceManager, entityRenderer);
    }
}
