package me.xiaro.fastmc.shared.util.collection

inline fun <T, reified R> Array<T>.mapArray(transform: (T) -> R): Array<R> {
    return Array(this.size) {
        transform.invoke(this[it])
    }
}