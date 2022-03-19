package me.luna.fastmc.mixin

import com.google.common.collect.EvictingQueue
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import net.minecraft.client.particle.EmitterParticle
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleTextureSheet
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Predicate
import kotlin.collections.ArrayList

interface IPatchedParticleManager {
    fun tickParticle0(particle: Particle)

    @Suppress("UnstableApiUsage")
    fun tick0(
        particles: MutableMap<ParticleTextureSheet, Queue<Particle>>,
        newEmitterParticles: Queue<EmitterParticle>,
        newParticles: Queue<Particle>
    ) {
        runBlocking {
            launch(FastMcCoreScope.context) {
                coroutineScope {
                    for (particleQueue in particles.values) {
                        if (particleQueue.isEmpty()) continue
                        launch(FastMcCoreScope.context) {
                            val list = FastObjectArrayList(particleQueue)
                            val queue = ConcurrentLinkedQueue<Particle>()

                            coroutineScope {
                                ParallelUtils.splitListIndex(
                                    list.size,
                                    blockForEach = { start, end ->
                                        launch(FastMcCoreScope.context) {
                                            for (i in start until end) {
                                                val particle = list[i]
                                                tickParticle0(particle)
                                                if (!particle.isAlive) {
                                                    queue.add(particle)
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            particleQueue.removeAll(ObjectOpenHashSet(queue))
                        }
                    }
                }

                if (!newParticles.isEmpty()) {
                    var particle = newParticles.poll()
                    while (particle != null) {
                        particles.computeIfAbsent(particle.type) { EvictingQueue.create(16384) }.add(particle)
                        particle = newParticles.poll()
                    }
                }
            }

            if (!newEmitterParticles.isEmpty()) {
                newEmitterParticles.removeIf {
                    it.tick()
                    !it.isAlive
                }
            }
        }
    }
}