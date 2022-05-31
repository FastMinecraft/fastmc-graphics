package me.luna.fastmc.shared.font

class FontRenderContext {
    var color = -1

    var bold = false; private set
    var italic = false; private set
    var underline = false; private set
    var strikethrough = false; private set

    fun checkFormatCode(text: CharSequence, index: Int): Boolean {
        if (index > 0 && text[index - 1] == '§') return true

        if (index < text.length - 1 && text[index] == '§') {
            when (text[index + 1]) {
                '0' -> color = 0x0
                '1' -> color = 0x1
                '2' -> color = 0x2
                '3' -> color = 0x3
                '4' -> color = 0x4
                '5' -> color = 0x5
                '6' -> color = 0x6
                '7' -> color = 0x7
                '8' -> color = 0x8
                '9' -> color = 0x9
                'a' -> color = 0xA
                'b' -> color = 0xB
                'c' -> color = 0xC
                'd' -> color = 0xD
                'e' -> color = 0xE
                'f' -> color = 0xF
                'l' -> bold = true
                'o' -> italic = true
                'n' -> underline = true
                'm' -> strikethrough = true
                'r' -> {
                    color = -1
                    bold = false
                    italic = false
                    underline = false
                    strikethrough = false
                }
            }

            return true
        }

        return false
    }
}