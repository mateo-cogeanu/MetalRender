package com.pebbles_boon.metalrender.sodium.mixins.iris;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Iris' shader-pack buttons on Minecraft's backend-neutral GUI path. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gui.element.screen.IrisButton",
    remap = false)
public abstract class IrisButtonMixin {
  @Redirect(
      method = "extractContents",
      at = @At(
          value = "INVOKE",
          target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_enableBlend(I)V"))
  private void metalrender$skipOpenGlBlend(int drawBuffer) {
    if (!MetalGpuBackend.isRequested()) {
      GlStateManager._enableBlend(drawBuffer);
    }
  }

  @Redirect(
      method = "extractContents",
      at = @At(
          value = "INVOKE",
          target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_enableDepthTest()V"))
  private void metalrender$skipOpenGlDepthTest() {
    if (!MetalGpuBackend.isRequested()) {
      GlStateManager._enableDepthTest();
    }
  }
}
