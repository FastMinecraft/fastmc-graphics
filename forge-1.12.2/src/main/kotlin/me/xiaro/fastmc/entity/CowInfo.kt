package me.xiaro.fastmc.entity

import me.xiaro.fastmc.shared.renderbuilder.entity.info.ICowInfo
import net.minecraft.entity.passive.EntityCow

interface CowInfo : LivingBaseInfo<EntityCow>, ICowInfo<EntityCow>