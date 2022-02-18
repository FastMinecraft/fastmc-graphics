package me.luna.fastmc.shared.util

import java.nio.Buffer

fun Buffer.skip(count: Int) {
    this.position(position() + count)
}