package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.instancing.entity.info.IEntityInfo

abstract class EntityInstancingRenderer<TE : Any>(worldRenderer: WorldRenderer) :
    AbstractInstancingRenderer<TE>(worldRenderer, IEntityInfo.registry)