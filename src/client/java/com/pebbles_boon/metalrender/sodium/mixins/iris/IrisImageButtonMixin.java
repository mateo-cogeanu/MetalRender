package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Iris' image buttons from mutating unavailable OpenGL state. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gui.OldImageButton", remap = false)
public abstract class IrisImageButtonMixin {
  @Redirect(
      method = "renderTexture",
      at = @At(
          value = "INVOKE",
          target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_enableDepthTest()V"))
  private void metalrender$skipOpenGlDepthTest() {
    if (!MetalGpuBackend.isRequested()) {
      GlStateManager._enableDepthTest();
    }
  }
}
