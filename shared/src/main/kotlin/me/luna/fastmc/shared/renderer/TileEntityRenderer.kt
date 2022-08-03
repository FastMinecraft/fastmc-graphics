package me.luna.fastmc.shared.renderer

import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo

abstract class TileEntityRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractRenderer<TE>(worldRenderer, ITileEntityInfo.registry)