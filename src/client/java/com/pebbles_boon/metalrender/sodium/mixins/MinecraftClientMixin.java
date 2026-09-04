package com.pebbles_boon.metalrender.sodium.mixins;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.performance.PerformanceController;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import com.pebbles_boon.metalrender.nativebridge.DirectMetalPresentationProbe;
import com.pebbles_boon.metalrender.util.MetalLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public class MinecraftClientMixin {
  @Unique private boolean metalrender$worldWasLoaded = false;
  @Unique private int metalrender$debugCounter = 0;
  @Inject(method = "runTick", at = @At("HEAD"), require = 0)
  private void metalrender$startFrame(boolean tick, CallbackInfo ci) {
    Minecraft client = (Minecraft)(Object)this;
    if (client.getWindow() != null) {
      DirectMetalPresentationProbe.initialize(client.getWindow().handle());
    }
    DirectMetalPresentationProbe.present();
    if (MetalRenderClient.isEnabled()) {
      PerformanceController.startFrame();
      ClientLevel world = client.level;
      metalrender$debugCounter++;
      if (metalrender$debugCounter % 600 == 1) {
        MetalLogger.info(
            "[MinecraftClientMixin] world=" +
            (world != null ? "present" : "null") +
            " worldWasLoaded=" + metalrender$worldWasLoaded + " wr=" +
            (MetalRenderClient.getWorldRenderer() != null ? "present"
                                                          : "null"));
      }
      if (world != null && !metalrender$worldWasLoaded) {
        MetalWorldRenderer wr = MetalRenderClient.getWorldRenderer();
        MetalLogger.info("[MinecraftClientMixin] World detected! wr=" +
                         (wr != null));
        if (wr != null) {
          try {
            wr.onWorldLoad();
            MetalLogger.info(
                "[MinecraftClientMixin] onWorldLoad() completed successfully");
          } catch (Exception e) {
            MetalLogger.error("[MinecraftClientMixin] onWorldLoad() FAILED", e);
          }
        }
        metalrender$worldWasLoaded = true;
      } else if (world == null && metalrender$worldWasLoaded) {
        MetalWorldRenderer wr = MetalRenderClient.getWorldRenderer();
        if (wr != null) {
          wr.onWorldUnload();
        }
        metalrender$worldWasLoaded = false;
      }
      if (world != null) {
        MetalWorldRenderer wr = MetalRenderClient.getWorldRenderer();
        if (wr != null && wr.shouldRenderWithMetal()) {
          wr.prepareMeshes();
        }
      }
    }
  }
  @Inject(method = "runTick", at = @At("TAIL"), require = 0)
  private void metalrender$endFrame(boolean tick, CallbackInfo ci) {
    if (MetalRenderClient.isEnabled()) {
      PerformanceController.endFrame();
    }
  }
}
