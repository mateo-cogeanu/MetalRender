package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies backend-neutral shader-pack macros without querying OpenGL. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.shader.StandardMacros", remap = false)
public abstract class IrisStandardMacrosMixin {
  @Inject(method = "getGlVersion", at = @At("HEAD"), cancellable = true)
  private static void metalrender$useSpirvShaderVersion(int name,
      CallbackInfoReturnable<String> callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.setReturnValue("460");
    }
  }

  @Inject(method = "getGlExtensions", at = @At("HEAD"), cancellable = true)
  private static void metalrender$useVulkanShaderFeatures(
      CallbackInfoReturnable<Set<String>> callback) {
    if (MetalGpuBackend.isRequested()) {
      callback.setReturnValue(Set.of());
    }
  }
}
