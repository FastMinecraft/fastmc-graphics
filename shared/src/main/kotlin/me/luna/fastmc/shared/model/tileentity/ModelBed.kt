package me.luna.fastmc.shared.model.tileentity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.model.ModelBuilder

class ModelBed : Model("tileEntity/Bed", 64, 64) {
    override fun ModelBuilder.buildModel() {
        // Head
        childModel {
            addBox(0.0f, 0.0f,-8.0f, 3.0f, -8.0f, 16.0f, 6.0f, 16.0f)
        }

        // Leg 0
        childModel {
            addBox(0.0f, 44.0f, -8.0f, 0.0f, -8.0f, 3.0f, 3.0f, 3.0f)
        }

        // Leg 1
        childModel {
            addBox(0.0f, 50.0f, 5.0f, 0.0f, -8.0f, 3.0f, 3.0f, 3.0f)
        }

        // Foot
        childModel {
            addBox(0.0f, 22.0f, -8.0f, 3.0f, -8.0f, 16.0f, 6.0f, 16.0f)
        }

        // Leg 2
        childModel {
            addBox(12.0f, 44.0f, -8.0f, 0.0f, 5.0f, 3.0f, 3.0f, 3.0f)
        }

        // Leg 3
        childModel {
            addBox(12.0f, 50.0f, 5.0f, 0.0f, 5.0f, 3.0f, 3.0f, 3.0f)
        }
    }
}