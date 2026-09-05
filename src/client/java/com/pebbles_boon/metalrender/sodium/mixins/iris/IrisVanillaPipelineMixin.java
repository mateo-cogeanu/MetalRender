package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoids resetting OpenGL state in Iris' Vulkan placeholder world pipeline. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.VanillaRenderingPipeline", remap = false)
public abstract class IrisVanillaPipelineMixin {
  @Inject(method = "beginLevelRendering", at = @At("HEAD"), cancellable = true, require = 0)
  private void metalrender$keepVulkanState(CallbackInfo callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.cancel();
    }
  }
}
