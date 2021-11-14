package me.xiaro.fastmc.shared.model.entity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.model.ModelBuilder

class ModelAlyx : Model("entity/Alyx",64, 64) {
    override fun ModelBuilder.buildModel() {
        // Head
        childModel {
            addBox(0.0f, 0.0f,-4.0f, 24.0f, -4.0f, 8.0f, 8.0f, 8.0f)
        }

        // Body
        childModel {
            addBox(16.0f, 16.0f, -4.0f, 12.0f, -2.0f, 8.0f, 12.0f, 4.0f)
        }

        // Left Arm
        childModel {
            addBox(32.0f, 48.0f, 4.0f, 12.0f, -2.0f, 3.0f, 12.0f, 4.0f)
        }

        // Right Arm
        childModel {
            addBox(40.0f, 16.0f, -8.0f, 12.0f, -2.0f, 3.0f, 12.0f, 4.0f)
        }

        // Left Leg
        childModel {
            addBox(16.0f, 48.0f, -4.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f)
        }

        // Right Leg
        childModel {
            addBox(0.0f, 16.0f, 0.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f)
        }
    }
}