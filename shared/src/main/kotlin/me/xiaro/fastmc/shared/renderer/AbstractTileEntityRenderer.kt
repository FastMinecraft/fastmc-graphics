package me.xiaro.fastmc.shared.renderer

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo

abstract class AbstractTileEntityRenderer<TE: Any>(worldRenderer: AbstractWorldRenderer) : AbstractRenderer<TE>(worldRenderer, ITileEntityInfo.registry)