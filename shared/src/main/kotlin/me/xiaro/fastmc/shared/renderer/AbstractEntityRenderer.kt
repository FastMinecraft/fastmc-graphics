package me.xiaro.fastmc.shared.renderer

import me.xiaro.fastmc.shared.renderbuilder.entity.info.IEntityInfo

abstract class AbstractEntityRenderer<TE: Any>(worldRenderer: AbstractWorldRenderer) : AbstractRenderer<TE>(worldRenderer, IEntityInfo.registry)