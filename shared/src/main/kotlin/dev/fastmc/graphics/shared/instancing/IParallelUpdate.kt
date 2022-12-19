package dev.fastmc.graphics.shared.instancing

interface IParallelUpdate {
    fun updateParallel(async: MutableList<Runnable>, callbacks: MutableList<Runnable>)
}