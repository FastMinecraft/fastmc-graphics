package dev.fastmc.graphics.mixin

class ListSet<T>(val wrapped: MutableList<T>) : MutableCollection<T> by wrapped, MutableSet<T>