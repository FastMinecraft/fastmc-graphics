package me.xiaro.fastmc.shared.model.tileentity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.model.ModelBuilder

class ModelLargeChest : Model("tileEntity/LargeChest", 128, 64) {
    override fun ModelBuilder.buildModel() {
        // Body
        childModel(0.0f, 19.0f) {
            addBox(-15.0f, 0.0f, -7.0f, 30.0f, 10.0f, 14.0f)
        }

        // Knob
        childModel {
            addBox(-1.0f, 7.0f, 7.0f, 2.0f, 4.0f, 1.0f)
        }

        // Lib
        childModel {
            addBox(-15.0f, 9.0f, -7.0f, 30.0f, 5.0f, 14.0f)
        }
    }
}