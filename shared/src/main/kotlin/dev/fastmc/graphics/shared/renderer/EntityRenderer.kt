package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.instancing.entity.info.IEntityInfo

abstract class EntityRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractRenderer<TE>(worldRenderer, IEntityInfo.registry)