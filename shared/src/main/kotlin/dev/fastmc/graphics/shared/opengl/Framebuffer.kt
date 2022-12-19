package dev.fastmc.graphics.shared.opengl

import dev.fastmc.common.collection.FastObjectArrayList

class Framebuffer : IGLObject, IGLTargetBinding {
    override val id = glCreateFramebuffers()
    val colorAttachments = FastObjectArrayList<TextureObject.Texture2D>()
    var depthAttachment: TextureObject.Texture2D? = null; private set
    var stencilAttachment: TextureObject.Texture2D? = null; private set

    fun attach(texture: TextureObject.Texture2D, attachment: Int, level: Int = 0) {
        when (attachment) {
            GL_DEPTH_ATTACHMENT -> {
                depthAttachment = texture
            }
            GL_STENCIL_ATTACHMENT -> {
                stencilAttachment = texture
            }
            GL_DEPTH_STENCIL_ATTACHMENT -> {
                depthAttachment = texture
                stencilAttachment = texture
            }
            else -> {
                colorAttachments.add(texture)
            }
        }
        glNamedFramebufferTexture(id, attachment, texture.id, level)
    }

    fun check() {
        require(glCheckNamedFramebufferStatus(id, GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)
    }

    override fun bind(target: Int) {
        glBindFramebuffer(target, id)
    }

    override fun unbind(target: Int) {
        glBindFramebuffer(target, 0)
    }

    fun destroyFbo() {
        glDeleteFramebuffers(id)
    }

    override fun destroy() {
        glDeleteFramebuffers(id)
        for (i in colorAttachments.indices) {
            colorAttachments[i].destroy()
        }
        val depth = depthAttachment
        val stencil = stencilAttachment
        if (depth != null) {
            depth.destroy()
        }
        if (stencil != null && stencil !== depth) {
            stencil.destroy()
        }
    }
}