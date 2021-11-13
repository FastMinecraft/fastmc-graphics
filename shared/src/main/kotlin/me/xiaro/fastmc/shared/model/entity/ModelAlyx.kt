package me.xiaro.fastmc.shared.model.entity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.model.ModelBuilder

class ModelAlyx : Model("entity/alyx",64, 64) {
    override fun ModelBuilder.buildModel() {
        // Head
        childModel {
            addBox(-4.0f, 24.0f, -4.0f, 8.0f, 8.0f, 8.0f)
        }

        // Body
        childModel(16.0f, 16.0f) {
            addBox(-4.0f, 12.0f, -2.0f, 8.0f, 12.0f, 4.0f)
        }

        // Left Arm
        childModel(32.0f, 48.0f) {
            addBox(4.0f, 12.0f, -2.0f, 3.0f, 12.0f, 4.0f)
        }

        // Right Arm
        childModel(40.0f, 16.0f) {
            addBox(-8.0f, 12.0f, -2.0f, 3.0f, 12.0f, 4.0f)
        }

        // Left Leg
        childModel(16.0f, 48.0f) {
            addBox(-4.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f)
        }

        // Right Leg
        childModel(0.0f, 16.0f) {
            addBox(0.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f)
        }
    }
}