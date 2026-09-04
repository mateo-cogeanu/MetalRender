package com.pebbles_boon.metalrender.sodium.mixins;

import com.mojang.blaze3d.systems.GpuBackend;
import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import java.util.Arrays;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PreferredGraphicsApi.class)
public abstract class PreferredGraphicsApiMixin {
  @Inject(method = "getBackendsToTry", at = @At("RETURN"), cancellable = true)
  private void metalrender$prependMetalBackend(
      CallbackInfoReturnable<GpuBackend[]> callback) {
    if (!Boolean.getBoolean(MetalGpuBackend.ENABLE_PROPERTY)
        || !System.getProperty("os.name", "").startsWith("Mac")) {
      return;
    }

    GpuBackend[] existing = callback.getReturnValue();
    GpuBackend[] withMetal = Arrays.copyOf(existing, existing.length + 1);
    System.arraycopy(existing, 0, withMetal, 1, existing.length);
    withMetal[0] = new MetalGpuBackend();
    callback.setReturnValue(withMetal);
  }
}
