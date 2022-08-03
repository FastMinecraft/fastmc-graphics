package me.luna.fastmc.shared.renderer

import me.luna.fastmc.shared.instancing.entity.info.IEntityInfo

abstract class EntityRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractRenderer<TE>(worldRenderer, IEntityInfo.registry)