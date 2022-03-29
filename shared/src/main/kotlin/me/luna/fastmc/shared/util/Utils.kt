package me.luna.fastmc.shared.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable

@JvmField
val EMPTY_RUNNABLE = Runnable {}

inline val Job?.isCompletedOrNull
    get() = this == null || this.isCompleted