package me.luna.fastmc.shared.opengl

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.luna.fastmc.shared.util.CachedBuffer
import me.luna.fastmc.shared.util.MD5Hash
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import java.nio.charset.CodingErrorAction
import java.security.DigestInputStream
import java.security.MessageDigest

sealed class ShaderSource(val codeSrc: CharSequence) {
    class Vertex private constructor(codeSrc: CharSequence) : ShaderSource(codeSrc) {
        companion object : Provider<Vertex>("vsh") {
            override fun newInstance(codeSrc: CharSequence): Vertex {
                return Vertex(codeSrc)
            }
        }
    }

    class Fragment private constructor(codeSrc: CharSequence) : ShaderSource(codeSrc) {
        companion object : Provider<Fragment>("fsh") {
            override fun newInstance(codeSrc: CharSequence): Fragment {
                return Fragment(codeSrc)
            }
        }
    }

    class Util private constructor(codeSrc: CharSequence) : ShaderSource(codeSrc) {
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
            return invoke(path, DefineBuilder().apply(block))
        }

        operator fun invoke(path: String, defines: DefineBuilder): T {
            val cache = getCache(path)
            val stringBuilder = StringBuilder()
            val firstLine = cache.lines[0]
            if (firstLine.startsWith("#version")) {
                stringBuilder.appendLine(firstLine)
                stringBuilder.append(defines)
            } else {
                stringBuilder.append(defines)
                stringBuilder.appendLine(firstLine)
            }

            for (i in 1 until cache.lines.size) {
                stringBuilder.appendLine(cache.lines[i])
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

    class DefineBuilder private constructor(private val stringBuilder: StringBuilder) : CharSequence by stringBuilder {
        constructor() : this(StringBuilder())

        fun define(name: String) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
        }

        fun define(name: String, value: Any) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.append(' ')
            stringBuilder.append(value.toString())
        }

        fun define(name: String, value: Boolean) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.append(' ')
            stringBuilder.append(value)
        }

        fun define(name: String, value: Int) {
            stringBuilder.append("#define")
            stringBuilder.append(' ')
            stringBuilder.append(name)
            stringBuilder.append(' ')
            stringBuilder.append(value)
        }
    }
}
