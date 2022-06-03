package me.luna.fastmc.shared.opengl

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.luna.fastmc.shared.util.CachedBuffer
import me.luna.fastmc.shared.util.MD5Hash
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import java.nio.charset.CodingErrorAction
import java.security.DigestInputStream
import java.security.MessageDigest

sealed class ShaderSource(val codeSrc: CharSequence) {
    private val lines by lazy { codeSrc.lines() }
    protected abstract val provider: Provider<*>

    class Vertex private constructor(codeSrc: CharSequence) : ShaderSource(codeSrc) {
        override val provider: Provider<Vertex> get() = Companion

        companion object : Provider<Vertex>("vsh") {
            override fun newInstance(codeSrc: CharSequence): Vertex {
                return Vertex(codeSrc)
            }
        }
    }

    class Fragment private constructor(codeSrc: CharSequence) : ShaderSource(codeSrc) {
        override val provider: Provider<Fragment> get() = Companion

        companion object : Provider<Fragment>("fsh") {
            override fun newInstance(codeSrc: CharSequence): Fragment {
                return Fragment(codeSrc)
            }
        }
    }

    class Util private constructor(codeSrc: CharSequence) : ShaderSource(codeSrc) {
        override val provider: Provider<Util> get() = Companion

        companion object : Provider<Util>("glsl") {
            override fun newInstance(codeSrc: CharSequence): Util {
                return Util(codeSrc)
            }
        }
    }

    abstract class Provider<T : ShaderSource> protected constructor(extension: String) {
        private val extension = ".$extension"
        private val cacheMap = Object2ObjectOpenHashMap<String, Cache>()

        protected abstract fun newInstance(codeSrc: CharSequence): T

        operator fun invoke(path: String): T {
            return getCache(path).instance
        }

        inline operator fun invoke(path: String, crossinline block: DefineBuilder.() -> Unit): T {
            return getWithDefines(path, DefineBuilder().apply(block))
        }

        fun getWithDefines(path: String, defines: DefineBuilder): T {
            return getWithDefines(getCache(path), defines)
        }

        private fun getWithDefines(cache: Cache, defines: DefineBuilder): T {
            return if (defines.stringBuilder.isEmpty()) {
                cache.instance
            } else {
                buildWithDefines(cache.lines, defines)
            }
        }

        fun buildWithDefines(lines: List<CharSequence>, defines: DefineBuilder): T {
            val stringBuilder = StringBuilder()

            var inserted = false

            for (i in lines.indices) {
                val line = lines[i]
                if (!inserted && !line.startsWith("#version") && !line.startsWith("#define")) {
                    stringBuilder.append(defines.stringBuilder)
                    inserted = true
                }
                stringBuilder.appendLine(line)
            }

            return newInstance(stringBuilder)
        }

        private fun getCache(path: String): Cache {
            if (!path.endsWith(extension)) {
                throw IllegalArgumentException("Invalid shader extension ($path)")
            }

            val url = javaClass.getResource(path) ?: throw IllegalArgumentException("Invalid shader path ($path)")
            val md5 = MessageDigest.getInstance("MD5")

            cachedByteBuffer.getByte().clear()
            var read = 0

            url.openStream().use { inputStream ->
                DigestInputStream(inputStream, md5).use {
                    var byte = it.read()
                    while (byte != -1) {
                        read++
                        cachedByteBuffer.ensureCapacityByte(read, (read + 1023) shr 10 shl 10).put(byte.toByte())
                        byte = it.read()
                    }
                }
            }
            val buffer = cachedByteBuffer.getByte()
            buffer.flip()
            if (!buffer.hasRemaining()) throw IllegalArgumentException("Shader file is empty ($path)")

            val hash = MD5Hash(md5.digest())

            var source = cacheMap[path]
            if (source == null || source.hash != hash) {
                val decoder = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)

                val lines = decoder.decode(buffer).lineSequence()
                    .mapTo(FastObjectArrayList()) {
                        if (it.startsWith("#import")) {
                            val importPath = it.substring(it.indexOf('/'))
                            val importContent = Util(importPath).codeSrc
                            importContent
                        } else {
                            it
                        }
                    }

                source = Cache(lines, hash)
                cacheMap[path] = source
            }

            return source
        }

        private inner class Cache(val lines: List<CharSequence>, val hash: MD5Hash) {
            val str by lazy {
                buildString {
                    for (i in lines.indices) {
                        appendLine(lines[i])
                    }
                }
            }
            val instance by lazy { newInstance(str) }
        }

        private companion object {
            private val cachedByteBuffer = CachedBuffer(1024)
        }
    }

    class DefineBuilder {
        internal val stringBuilder = StringBuilder()

        fun define(name: String) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.appendLine()
        }

        fun define(name: String, value: Any) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.append(' ')
            stringBuilder.append(value.toString())
            stringBuilder.appendLine()
        }

        fun define(name: String, value: Boolean) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.append(' ')
            stringBuilder.append(value)
            stringBuilder.appendLine()
        }

        fun define(name: String, value: Int) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.append(' ')
            stringBuilder.append(value)
            stringBuilder.appendLine()
        }
    }

    companion object {
        inline operator fun <T: ShaderSource> T.invoke(crossinline block: DefineBuilder.() -> Unit): T {
            return this.withDefines(DefineBuilder().apply(block))
        }

        fun <T: ShaderSource> T.withDefines(defines: DefineBuilder): T {
            return if (defines.stringBuilder.isEmpty()) {
                this
            } else {
                @Suppress("UNCHECKED_CAST")
                (this.provider as Provider<T>).buildWithDefines(this.lines, defines)
            }
        }
    }
}
