package me.luna.fastmc.shared.model.entity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.model.ModelBuilder

class ModelCow : Model("entity/Cow", 64, 64) {
    override fun ModelBuilder.buildModel() {
        // Head
        childModel {
            addBox(0.0f, 0.0f, -4.0f, 16.0f, 8.0f, 8.0f, 8.0f, 6.0f)

            childModel {
                textureOffset(0.0f, 0.0f) {
                    addBox(-5.0f, 22.0f, 11.0f, 1.0f, 3.0f, 1.0f)
                    addBox(4.0f, 22.0f, 11.0f, 1.0f, 3.0f, 1.0f)
                }
            }
        }

        // Body
        childModel {
            addBox(0.0f, 16.0f, -6.0F, 12.0F, -10.0F, 12.0f, 10.0f, 18.0f)

            childModel {
                addBox(0.0f, 44.0f, -2.0f, 11.0f, -10.0f, 4.0f, 1.0f, 6.0f)
            }
        }

        // Leg1
        childModel {
            addBox(28.0f, 0.0f,-6.0F, 0.0F, -9.0F, 4.0f, 12.0f, 4.0f)
        }

        // Leg2
        childModel {
            addBox(28.0f, 0.0f,2.0F, 0.0F, -9.0F, 4.0f, 12.0f, 4.0f)
        }

        // Leg3
        childModel {
            addBox(28.0f, 0.0f,-6.0F, 0.0F, 4.0F, 4.0f, 12.0f, 4.0f)
        }

        // Leg4
        childModel {
            addBox(28.0f, 0.0f,2.0F, 0.0F, 4.0F, 4.0f, 12.0f, 4.0f)
        }
    }
}