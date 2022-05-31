package me.luna.fastmc.shared.renderbuilder

interface IParallelUpdate {
    fun updateParallel(async: MutableList<Runnable>, callbacks: MutableList<Runnable>)
}