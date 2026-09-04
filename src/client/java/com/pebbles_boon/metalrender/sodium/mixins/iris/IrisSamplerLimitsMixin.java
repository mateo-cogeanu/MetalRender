package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.lwjgl.opengl.GL;

/**
 * Prevents Iris from querying an OpenGL context while the no-API Vulkan window
 * is being initialized.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.sampler.SamplerLimits", remap = false)
public abstract class IrisSamplerLimitsMixin {
  private static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
  private static final int GL_MAX_DRAW_BUFFERS = 0x8824;
  private static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = 0x90DD;

  @Redirect(
      method = "<init>",
      at = @At(
          value = "INVOKE",
          target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_getInteger(I)I"))
  private int metalrender$usePortableVulkanLimit(int parameter) {
    if (!MetalGpuBackend.isRequested()) {
      return GlStateManager._getInteger(parameter);
    }

    return switch (parameter) {
      case GL_MAX_TEXTURE_IMAGE_UNITS -> 32;
      case GL_MAX_DRAW_BUFFERS -> 8;
      case GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS -> 16;
      default -> 0;
    };
  }

  @Redirect(
      method = "<init>",
      at = @At(
          value = "INVOKE",
          target = "Lnet/irisshaders/iris/gl/IrisRenderSystem;supportsSSBO()Z"))
  private boolean metalrender$reportStorageBufferSupport() {
    if (!MetalGpuBackend.isRequested()) {
      var capabilities = GL.getCapabilities();
      return capabilities.OpenGL44
          || (capabilities.GL_ARB_shader_storage_buffer_object
              && capabilities.GL_ARB_buffer_storage);
    }
    return true;
  }
}
