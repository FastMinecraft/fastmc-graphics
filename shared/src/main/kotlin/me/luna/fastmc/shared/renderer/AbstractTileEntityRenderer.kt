package me.luna.fastmc.shared.renderer

import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo

abstract class AbstractTileEntityRenderer<TE: Any>(worldRenderer: AbstractWorldRenderer) : AbstractRenderer<TE>(worldRenderer, ITileEntityInfo.registry)