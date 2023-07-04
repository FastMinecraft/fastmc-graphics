package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo

abstract class TileEntityInstancingRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractInstancingRenderer<TE>(worldRenderer, ITileEntityInfo.registry)