package me.luna.fastmc.shared.util

import kotlinx.coroutines.Job

inline val Job?.isCompletedOrNull
    get() = this == null || this.isCompleted