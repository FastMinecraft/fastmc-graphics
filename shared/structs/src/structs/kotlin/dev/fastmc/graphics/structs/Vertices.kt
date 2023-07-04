package dev.fastmc.graphics.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
interface FontVertex {
    val position: Vec2f32
    val vertUV: Vec2i16
    val colorIndex: Byte
    val overrideColor: Byte
    val shadow: Byte
}