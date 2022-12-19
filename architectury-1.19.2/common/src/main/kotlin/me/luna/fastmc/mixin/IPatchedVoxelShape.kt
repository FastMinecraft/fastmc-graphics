package me.luna.fastmc.mixin

import net.minecraft.util.shape.FractionalDoubleList

interface IPatchedVoxelShape {
    companion object {
        private val cache = arrayOfNulls<FractionalDoubleList>(1024)

        @JvmStatic
        fun getFractionalDoubleList(sectionCount: Int): FractionalDoubleList {
            if (sectionCount >= 1024) {
                return FractionalDoubleList(sectionCount)
            }

            var value = cache[sectionCount]
            if (value == null) {
                value = FractionalDoubleList(sectionCount)
                cache[sectionCount] = value
            }

            return value
        }
    }
}