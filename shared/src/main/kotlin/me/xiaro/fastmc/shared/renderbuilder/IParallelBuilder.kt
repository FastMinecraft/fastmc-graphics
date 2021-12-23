package me.xiaro.fastmc.shared.renderbuilder

interface IParallelBuilder<T : IInfo<*>> : IBuilder<T> {
    fun addParallel(worker: ParallelBuilderWorker, info: T)
}