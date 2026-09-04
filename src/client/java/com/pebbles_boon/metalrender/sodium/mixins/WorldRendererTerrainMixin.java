package com.pebbles_boon.metalrender.sodium.mixins;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkSectionsToRender.class)
public class WorldRendererTerrainMixin {
  @Unique
  private boolean metalrender$maskedColor = false;
  @Inject(method = "renderSection", at = @At("HEAD"), cancellable = true, require = 0)
  private void metalrender$beginDepthOnly(ChunkSectionLayerGroup layerGroup,
      GpuSampler sampler, CallbackInfo ci) {
    if (MetalRenderClient.isEnabled()) {
      MetalWorldRenderer wr = MetalRenderClient.getWorldRenderer();
      if (wr != null && wr.shouldRenderWithMetal()) {
        GL11.glColorMask(false, false, false, false);
        metalrender$maskedColor = true;
      }
    }
  }
  @Inject(method = "renderSection", at = @At("RETURN"), require = 0)
  private void metalrender$endDepthOnly(ChunkSectionLayerGroup layerGroup,
      GpuSampler sampler, CallbackInfo ci) {
    if (metalrender$maskedColor) {
      GL11.glColorMask(true, true, true, true);
      metalrender$maskedColor = false;
    }
  }
}
