package dev.fastmc.graphics.shared.model.tileentity

import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.model.ModelBuilder

class ModelShulkerBox : Model("tileEntity/ShulkerBox", 64, 64) {
    override fun ModelBuilder.buildModel() {
        // Base
        childModel {
            addBox(0.0f, 28.0f, -8.0f, 0.0f, -8.0f, 16.0f, 8.0f, 16.0f)
        }

        // Lid
        childModel {
            addBox(0.0f, 0.0f, -8.0f, 4.0f, -8.0f, 16.0f, 12.0f, 16.0f)
        }
    }
}