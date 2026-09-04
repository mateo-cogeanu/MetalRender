package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import com.pebbles_boon.metalrender.util.MetalLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoids Iris' OpenGL-only bootstrap work on a Vulkan device. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public abstract class IrisInitializationMixin {
  @Inject(method = "duringRenderSystemInit", at = @At("HEAD"), cancellable = true)
  private static void metalrender$skipOpenGlDebugSetup(CallbackInfo callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.cancel();
    }
  }

  @Inject(method = "onRenderSystemInit", at = @At("HEAD"), cancellable = true)
  private static void metalrender$skipOpenGlDeviceSetup(CallbackInfo callback) {
    if (MetalGpuBackend.isRequested()) {
      MetalLogger.info("Iris detected: bypassing its OpenGL bootstrap while "
          + "the Vulkan shader-pack backend is under construction");
      callback.cancel();
    }
  }
}
