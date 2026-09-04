package com.pebbles_boon.metalrender.sodium.mixins;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.entity.MetalEntityRenderer;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import com.pebbles_boon.metalrender.util.MetalLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public class EntityRenderMixin {
  @Unique
  private int metalrender$entityCaptureCount = 0;
  @Inject(method = "fillEntityRenderStates", at = @At("TAIL"), require = 0)
  private void metalrender$captureEntities(Camera camera, Frustum frustum,
      DeltaTracker tickCounter,
      LevelRenderState worldRenderState,
      CallbackInfo ci) {
    if (!MetalRenderClient.isEnabled())
      return;
    MetalWorldRenderer worldRenderer = MetalRenderClient.getWorldRenderer();
    if (worldRenderer == null || !worldRenderer.shouldRenderWithMetal())
      return;
    MetalEntityRenderer entityRenderer = worldRenderer.getEntityRenderer();
    if (entityRenderer == null || !entityRenderer.isActive())
      return;
    Minecraft client = Minecraft.getInstance();
    if (client == null || client.level == null)
      return;
    float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
    try {
      int capturedThisFrame = 0;
      for (Entity entity : client.level.entitiesForRendering()) {
        if (entity == null || entity.isRemoved())
          continue;
        if (entity == camera.entity() && !camera.isDetached())
          continue;
        if (!frustum.isVisible(entity.getBoundingBox()))
          continue;
        Matrix4f modelMatrix = new Matrix4f();
        entityRenderer.captureEntity(entity, tickDelta, modelMatrix);
        capturedThisFrame++;
      }
      metalrender$entityCaptureCount += capturedThisFrame;
      if (capturedThisFrame > 0 &&
          (metalrender$entityCaptureCount <= 5 ||
              metalrender$entityCaptureCount % 1000 == 0)) {
        MetalLogger.info(
            "[EntityRenderMixin] Captured %d entities this frame (total: %d)",
            capturedThisFrame, metalrender$entityCaptureCount);
      }
    } catch (Exception e) {
      if (metalrender$entityCaptureCount < 10) {
        MetalLogger.error("[EntityRenderMixin] Failed to capture entities: %s",
            e.getMessage());
      }
    }
  }
}
