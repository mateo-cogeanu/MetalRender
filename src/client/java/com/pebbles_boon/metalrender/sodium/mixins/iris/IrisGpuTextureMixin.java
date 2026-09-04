package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.mojang.blaze3d.textures.GpuTexture;
import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Iris' optional GL texture hooks harmless on Vulkan textures. */
@Mixin(value = GpuTexture.class, priority = 900)
public abstract class IrisGpuTextureMixin {
  @Inject(
      method = "iris$getGlId",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0)
  private void metalrender$useNoOpenGlTextureId(
      CallbackInfoReturnable<Integer> callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.setReturnValue(0);
    }
  }

  @Inject(
      method = "iris$markMipmapNonLinear",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0)
  private void metalrender$ignoreOpenGlMipmapMarker(CallbackInfo callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.cancel();
    }
  }
}
