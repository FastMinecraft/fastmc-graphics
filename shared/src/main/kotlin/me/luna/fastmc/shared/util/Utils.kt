package me.luna.fastmc.shared.util

import kotlinx.coroutines.Job
import java.nio.Buffer

fun Buffer.skip(count: Int) {
    this.position(position() + count)
}

inline val Job?.isCompletedOrNull
    get() = this == null || this.isCompleted