package me.luna.fastmc.entity

import me.luna.fastmc.shared.instancing.entity.info.ICowInfo
import net.minecraft.entity.passive.EntityCow

interface CowInfo : LivingBaseInfo<EntityCow>, ICowInfo<EntityCow>