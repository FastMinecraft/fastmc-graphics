package me.xiaro.fastmc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import me.xiaro.fastmc.shared.util.BufferUtils
import java.nio.ByteBuffer
import java.util.function.Consumer

object TextureUpdater {
    private const val bufferPoolSize = 16

    private var updater0: Updater? = null
    private val bufferPool = Channel<ByteBuffer>(bufferPoolSize)

    val updater: Updater
        get() {
            var value = updater0
            if (value == null) {
                value = Updater()
                updater0 = value
            }
            return value
        }

    init {
        repeat(bufferPoolSize) {
            bufferPool.trySend(BufferUtils.byte(0x1000000)).getOrThrow()
        }
    }

    fun onTickPre() {
        updater0?.uploadBlocking()
        updater0 = null
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onTickPost() {
        updater0?.let {
            GlobalScope.launch(Dispatchers.Default) {
                it.start()
            }
        }
    }

    fun onRender() {
        updater0?.upload()
    }

    fun cancel() {
        updater0?.cancel()
        updater0 = null
    }

    private suspend fun retainBuffer(): ByteBuffer {
        return bufferPool.receive().apply {
            clear()
        }
    }

    private fun releaseBuffer(buffer: ByteBuffer) {
        bufferPool.trySend(buffer).getOrThrow()
    }

    class Updater {
        private val tasks = ArrayList<UpdateTask>()
        private val channel = Channel<UploadCallback>(Channel.UNLIMITED)

        suspend fun start() {
            coroutineScope {
                for (task in tasks) {
                    val buffer = retainBuffer()
                    launch {
                        task.async.accept(buffer)
                        channel.trySend(UploadCallback(buffer, task.upload)).onFailure {
                            releaseBuffer(buffer)
                        }
                    }
                }
            }

            channel.close()
        }

        fun upload() {
            var callback = channel.tryReceive().getOrNull()
            while (callback != null) {
                callback.run()
                callback = channel.tryReceive().getOrNull()
            }
        }

        fun uploadBlocking() {
            runBlocking {
                for (callback in channel) {
                    callback.run()
                }
            }
        }

        fun newTask(async: Consumer<ByteBuffer>, upload: Consumer<ByteBuffer>) {
            tasks.add(UpdateTask(async, upload))
        }

        fun cancel() {
            channel.close()
            runBlocking {
                for (callback in channel) {
                    releaseBuffer(callback.buffer)
                }
            }
        }

        private class UpdateTask(val async: Consumer<ByteBuffer>, val upload: Consumer<ByteBuffer>)
    }


    private class UploadCallback(val buffer: ByteBuffer, private val uploadCallback: Consumer<ByteBuffer>) : Runnable {
        override fun run() {
            uploadCallback.accept(buffer)
            releaseBuffer(buffer)
        }
    }
}