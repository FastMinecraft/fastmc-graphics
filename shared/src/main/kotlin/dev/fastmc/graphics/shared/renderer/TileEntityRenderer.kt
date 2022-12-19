package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo

abstract class TileEntityRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractRenderer<TE>(worldRenderer, ITileEntityInfo.registry)