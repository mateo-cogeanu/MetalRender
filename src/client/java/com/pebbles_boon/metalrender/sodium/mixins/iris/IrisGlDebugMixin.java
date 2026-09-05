package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Iris' optional OpenGL debug labels out of the Vulkan render path. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.GLDebug", remap = false)
public abstract class IrisGlDebugMixin {
  @Inject(
      method = {"nameObject", "pushGroup", "popGroup"},
      at = @At("HEAD"),
      cancellable = true,
      require = 0)
  private static void metalrender$skipOpenGlDebugMarkers(CallbackInfo callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.cancel();
    }
  }
}
