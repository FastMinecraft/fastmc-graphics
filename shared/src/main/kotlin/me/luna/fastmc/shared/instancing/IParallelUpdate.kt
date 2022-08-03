package me.luna.fastmc.shared.instancing

interface IParallelUpdate {
    fun updateParallel(async: MutableList<Runnable>, callbacks: MutableList<Runnable>)
}