package me.luna.fastmc.shared.renderer

import me.luna.fastmc.shared.renderbuilder.entity.info.IEntityInfo

abstract class EntityRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractRenderer<TE>(worldRenderer, IEntityInfo.registry)