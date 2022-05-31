package me.luna.fastmc.shared.model.tileentity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.model.ModelBuilder

class ModelLargeChest : Model("tileEntity/LargeChest", 128, 64) {
    override fun ModelBuilder.buildModel() {
        // Body
        childModel {
            addBox(0.0f, 19.0f, -15.0f, 0.0f, -7.0f, 30.0f, 10.0f, 14.0f)
        }

        // Knob
        childModel {
            addBox(0.0f, 0.0f, -1.0f, 7.0f, 7.0f, 2.0f, 4.0f, 1.0f)
        }

        // Lib
        childModel {
            addBox(0.0f, 0.0f, -15.0f, 9.0f, -7.0f, 30.0f, 5.0f, 14.0f)
        }
    }
}