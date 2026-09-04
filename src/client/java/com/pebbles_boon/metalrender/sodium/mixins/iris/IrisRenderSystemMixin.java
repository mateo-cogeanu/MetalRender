package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents Iris from initializing OpenGL-only renderer state on Vulkan. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.IrisRenderSystem", remap = false)
public abstract class IrisRenderSystemMixin {
  @Inject(method = "initRenderer", at = @At("HEAD"), cancellable = true)
  private static void metalrender$skipOpenGlRendererInitialization(
      CallbackInfo callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.cancel();
    }
  }
}
