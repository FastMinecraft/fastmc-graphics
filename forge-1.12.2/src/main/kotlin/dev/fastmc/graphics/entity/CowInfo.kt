package dev.fastmc.graphics.entity

import dev.fastmc.graphics.shared.instancing.entity.info.ICowInfo
import net.minecraft.entity.passive.EntityCow

interface CowInfo : LivingBaseInfo<EntityCow>, ICowInfo<EntityCow>