package me.luna.fastmc.shared.util

import kotlinx.coroutines.Job
import java.util.*
import java.util.concurrent.Future
import kotlin.coroutines.Continuation

const val BYTE_UNCHECKED: Byte = 0
const val BYTE_FALSE: Byte = 1
const val BYTE_TRUE: Byte = 2

interface Cancellable {
    val isCancelled: Boolean
}

@JvmField
val EMPTY_RUNNABLE = Runnable {}

@JvmField
val EMPTY_LONG_ARRAY = LongArray(0)

@JvmField
val EMPTY_INT_ARRAY = IntArray(0)

inline val Cancellable?.isCancelledOrNull
    get() = this == null || this.isCancelled

inline val Job?.isCancelledOrNull
    get() = this == null || this.isCancelled

inline val Job?.isCompletedOrNull
    get() = this == null || this.isCompleted

inline val Future<*>?.isDoneOrNull: Boolean
    get() = this == null || this.isDone

private val UNIT_RESULT = Result.success(Unit)
fun Continuation<Unit>.resume() {
    resumeWith(UNIT_RESULT)
}

inline fun <T> Queue<T>.pollEach(block: (T) -> Unit) {
    var e = this.poll()
    while (e != null) {
        block.invoke(e)
        e = this.poll()
    }
}