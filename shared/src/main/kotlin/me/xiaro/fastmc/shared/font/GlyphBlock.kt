package me.xiaro.fastmc.shared.font

class GlyphBlock(
    val texture: GlyphTexture,
    val glyphs: Array<CharGlyph>,
    val unicode: Boolean
)