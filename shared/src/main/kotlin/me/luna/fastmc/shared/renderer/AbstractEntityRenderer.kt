package me.luna.fastmc.shared.renderer

import me.luna.fastmc.shared.renderbuilder.entity.info.IEntityInfo

abstract class AbstractEntityRenderer<TE : Any>(worldRenderer: AbstractWorldRenderer) :
    AbstractRenderer<TE>(worldRenderer, IEntityInfo.registry)