package com.pebbles_boon.metalrender.sodium.mixins;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.backend.MetalRenderer;
import com.pebbles_boon.metalrender.render.CapturedMatrices;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import com.pebbles_boon.metalrender.util.MetalLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import com.mojang.blaze3d.opengl.GlTexture;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public class WorldRendererBlitMixin {
  @Unique
  private int metalrender$blitFrameCount = 0;
  @Unique
  private int metalrender$tmpFbo = 0;
  @Unique
  private long metalrender$renderEndNanos = 0;
  @Unique
  private long metalrender$javaProfileAcc = 0;
  @Unique
  private int metalrender$javaProfileCount = 0;

  @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true, require = 0)
  private void metalrender$doMetalRender(GraphicsResourceAllocator resourceAllocator,
      DeltaTracker deltaTracker,
      boolean renderOutline,
      CameraRenderState cameraState,
      Matrix4fc modelViewMatrix,
      GpuBufferSlice terrainFog,
      Vector4f fogColor,
      boolean shouldRenderSky,
      ChunkSectionsToRender chunkSectionsToRender,
      CallbackInfo ci) {
    if (!MetalRenderClient.isEnabled())
      return;
    MetalWorldRenderer worldRenderer = MetalRenderClient.getWorldRenderer();
    if (worldRenderer == null || !worldRenderer.shouldRenderWithMetal())
      return;
    try {
      Minecraft client = Minecraft.getInstance();
      Camera camera = client.gameRenderer.mainCamera();
      if (camera == null || camera.position() == null)
        return;
      float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
      float fov = client.options.fov().get().floatValue();
      // MC 26.1: getBasicProjectionMatrix may not exist - compute manually
      float aspectRatio = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();
      Matrix4f proj = new Matrix4f();
      proj.setPerspective((float) Math.toRadians(fov), aspectRatio, 0.05f, client.options.getEffectiveRenderDistance() * 16.0f * 4.0f);
      Matrix4f mv = new Matrix4f(modelViewMatrix);
      CapturedMatrices.capture(proj, mv, camera.position().x,
          camera.position().y,
          camera.position().z);
      worldRenderer.beginFrame(camera, tickDelta, new Matrix4f(proj),
          new Matrix4f(mv));
      worldRenderer.endFrame();
      metalrender$renderEndNanos = System.nanoTime();
    } catch (Exception e) {
      MetalLogger.error("[WorldRendererBlitMixin] Metal render failed: %s",
          e.getMessage());
    }
  }

  @Inject(method = "renderLevel", at = @At("RETURN"), require = 0)
  private void metalrender$addBlitAfterRender(GraphicsResourceAllocator resourceAllocator,
      DeltaTracker deltaTracker,
      boolean renderOutline,
      CameraRenderState cameraState,
      Matrix4fc modelViewMatrix,
      GpuBufferSlice terrainFog,
      Vector4f fogColor,
      boolean shouldRenderSky,
      ChunkSectionsToRender chunkSectionsToRender,
      CallbackInfo ci) {
    MetalWorldRenderer worldRenderer = MetalRenderClient.getWorldRenderer();
    if (worldRenderer == null || !worldRenderer.shouldRenderWithMetal())
      return;
    long blitStartNanos = System.nanoTime();
    long mcGapNanos = (metalrender$renderEndNanos > 0)
        ? (blitStartNanos - metalrender$renderEndNanos)
        : 0;
    com.mojang.blaze3d.pipeline.RenderTarget mainFb =
        Minecraft.getInstance().gameRenderer.mainRenderTarget();
    int prevDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    boolean boundColorFbo = false;
    Minecraft client = Minecraft.getInstance();
    int fbWidth = client.getWindow().getWidth();
    int fbHeight = client.getWindow().getHeight();
    if (mainFb != null && mainFb.getColorTexture() instanceof GlTexture glTex) {
      int texId = glTex.glId();
      if (metalrender$tmpFbo == 0) {
        metalrender$tmpFbo = GL30.glGenFramebuffers();
      }
      GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, metalrender$tmpFbo);
      GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,
          GL30.GL_COLOR_ATTACHMENT0,
          GL11.GL_TEXTURE_2D, texId, 0);
      int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
      if (status == GL30.GL_FRAMEBUFFER_COMPLETE) {
        boundColorFbo = true;
      } else {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
      }
    }
    {
      MetalRenderer renderer = MetalRenderClient.getRenderer();
      long handle = (renderer != null) ? renderer.getHandle() : 0;
      if (handle != 0) {
        int built = worldRenderer.buildMeshesDuringWait(handle);
        if (built > 0 && (metalrender$blitFrameCount % 120 == 0)) {
          MetalLogger.info("WAIT_BUILD: built %d meshes during GPU wait", built);
        }
      }
    }
    worldRenderer.forceBlitNow();
    if (boundColorFbo) {
      GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
    }
    boolean didDepth = false;
    long blitEndNanos = System.nanoTime();
    double blitMs = (blitEndNanos - blitStartNanos) / 1_000_000.0;
    double gapMs = mcGapNanos / 1_000_000.0;
    metalrender$javaProfileAcc += (blitEndNanos - blitStartNanos) + mcGapNanos;
    metalrender$javaProfileCount++;
    if (metalrender$javaProfileCount >= 120) {
      double avgTotal = (double) metalrender$javaProfileAcc / metalrender$javaProfileCount / 1_000_000.0;
      MetalLogger.info("JAVA_PROFILE: mcGap=%.2fms blit=%.2fms (avg over %d frames)",
          gapMs, blitMs, metalrender$javaProfileCount);
      metalrender$javaProfileAcc = 0;
      metalrender$javaProfileCount = 0;
    }
    metalrender$blitFrameCount++;
    if (metalrender$blitFrameCount <= 3 ||
        metalrender$blitFrameCount % 600 == 0) {
      MetalLogger.info(
          "[WorldRendererBlitMixin] Blit pass (frame %d, color=%s, depth=%s, "
              + "drawFBO=%d, %dx%d)",
          metalrender$blitFrameCount, boundColorFbo, didDepth, prevDrawFbo,
          fbWidth, fbHeight);
    }
  }
}
