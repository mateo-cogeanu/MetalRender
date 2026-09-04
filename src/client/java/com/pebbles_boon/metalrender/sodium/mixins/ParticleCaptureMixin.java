package com.pebbles_boon.metalrender.sodium.mixins;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.particle.MetalParticleRenderer;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import com.pebbles_boon.metalrender.util.MetalLogger;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public class ParticleCaptureMixin {
  @Unique
  private int metalrender$captureFrameCount = 0;

  // TODO: MC 26.1 removed SubmittableBatch and changed particle rendering pipeline.
  // The addToBatch method signature has changed. This mixin needs to target the
  // correct method in the new particle system. For now, try to inject into
  // a method that still exists.
  @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
  private void metalrender$captureAndCancelParticles(CallbackInfo ci) {
    if (!MetalRenderClient.isEnabled())
      return;
    MetalWorldRenderer worldRenderer = MetalRenderClient.getWorldRenderer();
    if (worldRenderer == null || !worldRenderer.shouldRenderWithMetal())
      return;
    MetalParticleRenderer particleRenderer = worldRenderer.getParticleRenderer();
    if (particleRenderer == null || !particleRenderer.isActive())
      return;
    Minecraft client = Minecraft.getInstance();
    Camera camera = client.gameRenderer.mainCamera();
    float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    particleRenderer.captureParticles((ParticleEngine) (Object) this, camera,
        tickDelta);
    metalrender$captureFrameCount++;
    if (metalrender$captureFrameCount <= 3 ||
        metalrender$captureFrameCount % 500 == 0) {
      MetalLogger.info(
          "[ParticleCaptureMixin] Captured particles frame %d, cancelling GL",
          metalrender$captureFrameCount);
    }
    ci.cancel();
  }
}
