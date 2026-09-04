package com.pebbles_boon.metalrender.entity;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.backend.MetalRenderer;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;
import com.pebbles_boon.metalrender.render.CapturedMatrices;
import com.pebbles_boon.metalrender.util.MetalLogger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
public class MetalEntityRenderer {
  private static final int ENTITY_VERTEX_STRIDE = 32;
  private static final int MAX_BATCH_VERTICES = 262144;
  private static final int MAX_ENTITIES_PER_FRAME = 512;
  private long deviceHandle;
  private boolean active;
  private int frameCount;
  private long lastEntityLogTime = 0;
  private int captureCallsPerSec = 0;
  private int renderCallsPerSec = 0;
  private int entitiesCapturedPerSec = 0;
  private ByteBuffer vertexStagingBuffer;
  private long metalVertexBuffer;
  private int currentVertexCount;
  private MetalVertexConsumer metalVertexConsumer;
  private final List<EntityDrawCommand> pendingDraws = new ArrayList<>();
  private final ConcurrentLinkedQueue<CapturedEntity> capturedEntities = new ConcurrentLinkedQueue<>();
  private final java.util.Map<Integer, Long> textureCache = new java.util.concurrent.ConcurrentHashMap<>();
  private PoseStack matrixStack = new PoseStack();
  public MetalEntityRenderer() {
    vertexStagingBuffer = ByteBuffer.allocateDirect(MAX_BATCH_VERTICES * ENTITY_VERTEX_STRIDE)
        .order(ByteOrder.nativeOrder());
    metalVertexConsumer = new MetalVertexConsumer(vertexStagingBuffer, MAX_BATCH_VERTICES);
    MetalLogger.info("[BUILD_V8] MetalEntityRenderer constructed - perf/water/nonfull fixes");
  }
  public void setDeviceAndPipeline(long device, long pipeline) {
    this.deviceHandle = device;
    if (device != 0) {
      metalVertexBuffer = NativeBridge.nCreateBuffer(
          device, MAX_BATCH_VERTICES * ENTITY_VERTEX_STRIDE, 0);
      MetalLogger.info("[BUILD_V8] MetalEntityRenderer initialized: device=%d vb=%d",
          device, metalVertexBuffer);
    }
  }
  public void setActive(boolean active) {
    this.active = active;
  }
  public boolean isActive() {
    return active;
  }
  public void captureEntity(Entity entity, float tickDelta,
      Matrix4f modelMatrix) {
    if (!active || entity == null)
      return;
    if (capturedEntities.size() >= MAX_ENTITIES_PER_FRAME)
      return;
    entitiesCapturedPerSec++;
    captureCallsPerSec++;
    CapturedEntity captured = new CapturedEntity();
    captured.entity = entity;
    captured.tickDelta = tickDelta;
    captured.modelMatrix = new Matrix4f(modelMatrix);
    captured.isHurt = entity instanceof LivingEntity le && le.hurtTime > 0;
    captured.hurtFactor = captured.isHurt ? ((LivingEntity) entity).hurtTime / 10.0f : 0.0f;
    capturedEntities.add(captured);
  }
  @SuppressWarnings("unchecked")
  public void buildEntityMeshes(long frameContext) {
    if (!active || frameContext == 0 || deviceHandle == 0)
      return;
    metalVertexConsumer.reset();
    currentVertexCount = 0;
    pendingDraws.clear();
    Minecraft client = Minecraft.getInstance();
    if (client == null || client.level == null)
      return;
    EntityRenderDispatcher renderManager = client.getEntityRenderDispatcher();
    if (renderManager == null)
      return;
    Camera camera = client.gameRenderer.mainCamera();
    double camX, camY, camZ;
    float currentTickDelta;
    if (camera != null && camera.position() != null) {
      camX = camera.position().x;
      camY = camera.position().y;
      camZ = camera.position().z;
    } else if (CapturedMatrices.isValid()) {
      camX = CapturedMatrices.getCamX();
      camY = CapturedMatrices.getCamY();
      camZ = CapturedMatrices.getCamZ();
    } else {
      camX = camY = camZ = 0;
    }
    currentTickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    int entitiesRendered = 0;
    int modelCaptures = 0;
    int boxFallbacks = 0;
    while (!capturedEntities.isEmpty()) {
      CapturedEntity captured = capturedEntities.poll();
      if (captured == null)
        break;
      Entity entity = captured.entity;
      if (entity == null || !entity.isAlive())
        continue;
      int startVertex = metalVertexConsumer.getVertexCount();
      boolean usedModel = false;
      try {
        usedModel = renderEntityModel(entity, captured, renderManager, camX,
            camY, camZ, currentTickDelta);
      } catch (Exception e) {
        if (frameCount < 5) {
          MetalLogger.warn("Failed to render entity model for %s: %s",
              entity.getType().toString(), e.getMessage());
        }
      }
      if (!usedModel || metalVertexConsumer.getVertexCount() == startVertex) {
        buildEntityQuads(entity, captured, camX, camY, camZ);
        boxFallbacks++;
      } else {
        modelCaptures++;
      }
      int verticesAdded = metalVertexConsumer.getVertexCount() - startVertex;
      if (verticesAdded > 0) {
        EntityDrawCommand cmd = new EntityDrawCommand();
        cmd.startVertex = startVertex;
        cmd.vertexCount = verticesAdded;
        cmd.hurtFactor = captured.hurtFactor;
        cmd.whiteFlash = 0.0f;
        cmd.renderFlags = 0;
        cmd.glTextureId = captured.glTextureId;
        pendingDraws.add(cmd);
        entitiesRendered++;
      }
    }
    currentVertexCount = metalVertexConsumer.getVertexCount();
    if (frameCount < 3 && entitiesRendered > 0) {
      MetalLogger.info(
          "[BUILD_V8] buildEntityMeshes: %d entities (%d model, %d box) -> %d verts",
          entitiesRendered, modelCaptures, boxFallbacks, currentVertexCount);
    }
  }
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private boolean renderEntityModel(Entity entity, CapturedEntity captured,
      EntityRenderDispatcher renderManager,
      double camX, double camY, double camZ,
      float tickDelta) {
    @SuppressWarnings("unchecked")
    EntityRenderer renderer = (EntityRenderer) renderManager.getRenderer(entity);
    if (renderer == null)
      return false;
    if (!(renderer instanceof LivingEntityRenderer livingRenderer))
      return false;
    EntityRenderState state;
    try {
      // MC 26.1: EntityRenderer uses createRenderState() + extractRenderState()
      state = livingRenderer.createRenderState();
      livingRenderer.extractRenderState(entity, state, tickDelta);
    } catch (Exception e) {
      return false;
    }
    if (state == null)
      return false;
    // Use a fresh PoseStack for each entity
    matrixStack = new PoseStack();
    // MC 26.1: Use manual lerp for entity position
    Vec3 lerpedPos = new Vec3(
        Mth.lerp(tickDelta, entity.xo, entity.getX()),
        Mth.lerp(tickDelta, entity.yo, entity.getY()),
        Mth.lerp(tickDelta, entity.zo, entity.getZ()));
    double ex = lerpedPos.x - camX;
    double ey = lerpedPos.y - camY;
    double ez = lerpedPos.z - camZ;
    Vec3 offset;
    try {
      offset = renderer.getRenderOffset(state);
    } catch (Exception e) {
      offset = Vec3.ZERO;
    }
    ex += offset.x;
    ey += offset.y;
    ez += offset.z;
    matrixStack.translate((float) ex, (float) ey, (float) ez);
    if (state instanceof LivingEntityRenderState livingState) {
      // MC 26.1: LivingEntityRenderState field names changed
      float bodyYaw = livingState.bodyRot;
      float baseScale = livingState.scale;
      matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
      matrixStack.scale(-1.0f, -1.0f, 1.0f);
      matrixStack.scale(baseScale, baseScale, baseScale);
      if (frameCount % 500 == 1) {
        MetalLogger.info("[ENTITY_DIAG_V7] entity=%s bodyYaw=%.1f baseScale=%.2f livingState=true",
            entity.getType().toString(), bodyYaw, baseScale);
      }
    } else {
      matrixStack.scale(-1.0f, -1.0f, 1.0f);
    }
    matrixStack.translate(0.0f, -1.501f, 0.0f);
    Model model = livingRenderer.getModel();
    if (model == null)
      return false;
    try {
      // TODO: MC 26.1 removed EntityModel.setAngles - model posing handled by render state
      model.setupAnim(state);
    } catch (Exception e) {
    }
    int light = 0x00F000F0; // TODO: MC 26.1 EntityRenderState light access changed
    if (light == 0) {
      light = 0x00F000F0;
    }
    model.renderToBuffer(matrixStack, metalVertexConsumer, light,
        OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    try {
      if (state instanceof LivingEntityRenderState livingState) {
        @SuppressWarnings("unchecked")
        LivingEntityRenderer typedRenderer = livingRenderer;
        Identifier texId = null;
        try {
          // MC 26.1: texture field may be on specific subclasses or accessed via reflection
          var field = livingState.getClass().getField("texture");
          Object val = field.get(livingState);
          if (val instanceof Identifier id) texId = id;
        } catch (Exception ignored) {}
        if (texId != null) {
          AbstractTexture mcTex = Minecraft.getInstance().getTextureManager().getTexture(
              texId);
          if (mcTex != null) {
            // MC 26.1: getTextureView() returns GpuTextureView, .texture() gives GlTexture
            try {
              var texView = mcTex.getTextureView();
              if (texView instanceof GpuTextureView gpuView && gpuView.texture() instanceof GlTexture glTex) {
                captured.glTextureId = glTex.glId();
              }
            } catch (Exception ignored) {}
          }
        }
      }
    } catch (Exception e) {
    }
    return true;
  }
  private void buildEntityQuads(Entity entity, CapturedEntity captured,
      double camX, double camY, double camZ) {
    float tickDelta = captured.tickDelta;
    float ex = (float) (Mth.lerp(tickDelta, entity.xo, entity.getX()) - camX);
    float ey = (float) (Mth.lerp(tickDelta, entity.yo, entity.getY()) - camY);
    float ez = (float) (Mth.lerp(tickDelta, entity.zo, entity.getZ()) - camZ);
    float halfW = (float)entity.getBoundingBox().getXsize() * 0.5f;
    float height = (float)entity.getBoundingBox().getYsize();
    float x0 = ex - halfW, y0 = ey, z0 = ez - halfW;
    float x1 = ex + halfW, y1 = ey + height, z1 = ez + halfW;
    int color = 0xFFFFFFFF;
    if (captured.isHurt) {
      int gb = (int) (255 * (1.0f - captured.hurtFactor * 0.6f));
      color = (255 << 24) | (255 << 16) | (gb << 8) | gb;
    }
    int light = 0x00F000F0;
    emitQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, color,
        light);
    emitQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, color,
        light);
    emitQuad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, color,
        light);
    emitQuad(x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, color,
        light);
    emitQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, color,
        light);
    emitQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, color,
        light);
  }
  private void emitQuad(float x0, float y0, float z0, float x1, float y1,
      float z1, float x2, float y2, float z2, float x3,
      float y3, float z3, float nx, float ny, float nz,
      int color, int light) {
    metalVertexConsumer.vertex(x0, y0, z0, color, 0, 0, 0, light, nx, ny, nz);
    metalVertexConsumer.vertex(x1, y1, z1, color, 1, 0, 0, light, nx, ny, nz);
    metalVertexConsumer.vertex(x2, y2, z2, color, 1, 1, 0, light, nx, ny, nz);
    metalVertexConsumer.vertex(x3, y3, z3, color, 0, 1, 0, light, nx, ny, nz);
  }
  public void renderCapturedEntities(long frameContext) {
    if (!active || frameContext == 0 || deviceHandle == 0)
      return;
    renderCallsPerSec++;
    long now = System.currentTimeMillis();
    if (now - lastEntityLogTime >= 10000) {
      MetalLogger.info(
          "Entity stats: captures=%d/10s renders=%d/10s entities=%d/10s",
          captureCallsPerSec, renderCallsPerSec, entitiesCapturedPerSec);
      captureCallsPerSec = 0;
      renderCallsPerSec = 0;
      entitiesCapturedPerSec = 0;
      lastEntityLogTime = now;
    }
    buildEntityMeshes(frameContext);
    if (currentVertexCount == 0 || pendingDraws.isEmpty())
      return;
    vertexStagingBuffer.flip();
    int uploadSize = currentVertexCount * ENTITY_VERTEX_STRIDE;
    if (metalVertexBuffer != 0 && uploadSize > 0) {
      byte[] data = new byte[uploadSize];
      vertexStagingBuffer.get(data);
      NativeBridge.nUploadBufferData(metalVertexBuffer, data, 0, uploadSize);
    }
    MetalRenderer renderer = MetalRenderClient.getRenderer();
    if (renderer == null) {
      if (frameCount < 5)
        MetalLogger.warn("MetalEntityRenderer: renderer null");
      return;
    }
    long entityPipeline = NativeBridge.nGetEntityPipelineHandle(renderer.getHandle());
    if (entityPipeline != 0) {
      NativeBridge.nSetPipelineState(frameContext, entityPipeline);
      if (frameCount < 3) {
        MetalLogger.info("Using entity pipeline: %d", entityPipeline);
      }
    } else {
      long inhousePipeline = renderer.getBackend().getInhousePipelineHandle();
      if (inhousePipeline == 0) {
        if (frameCount < 5)
          MetalLogger.warn("MetalEntityRenderer: no pipeline available");
      } else {
        NativeBridge.nSetPipelineState(frameContext, inhousePipeline);
        if (frameCount < 3) {
          MetalLogger.warn("FALLING BACK to terrain pipeline for entities! " +
              "entityPipeline=0");
        }
      }
    }
    NativeBridge.nSetChunkOffset(frameContext, 0.0f, 0.0f, 0.0f);
    int drawsDone = 0;
    for (EntityDrawCommand cmd : pendingDraws) {
      if (cmd.vertexCount <= 0)
        continue;
      NativeBridge.nSetEntityOverlay(frameContext, cmd.hurtFactor,
          cmd.whiteFlash, 1.0f);
      if (cmd.glTextureId != 0) {
        long metalTex = getOrCreateMetalTexture(cmd.glTextureId);
        if (metalTex != 0) {
          NativeBridge.nBindEntityTexture(frameContext, metalTex);
        }
        if (frameCount < 3) {
          MetalLogger.info("Entity draw: glTexId=%d metalTex=%d verts=%d",
              cmd.glTextureId, metalTex, cmd.vertexCount);
        }
      } else {
        if (frameCount < 3) {
          MetalLogger.info("Entity draw: NO texture, verts=%d",
              cmd.vertexCount);
        }
      }
      NativeBridge.nDrawEntityBuffer(frameContext, metalVertexBuffer,
          cmd.vertexCount, cmd.startVertex,
          cmd.renderFlags);
      drawsDone++;
    }
    frameCount++;
    if (frameCount <= 5 || frameCount % 500 == 0) {
      MetalLogger.info(
          "MetalEntityRenderer: frame %d, %d entities, %d verts, %d draws",
          frameCount, pendingDraws.size(), currentVertexCount, drawsDone);
    }
    pendingDraws.clear();
    capturedEntities.clear();
  }
  private long getOrCreateMetalTexture(int glTextureId) {
    if (glTextureId == 0 || deviceHandle == 0)
      return 0;
    Long cached = textureCache.get(glTextureId);
    if (cached != null)
      return cached;
    try {
      int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);
      int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0,
          GL11.GL_TEXTURE_WIDTH);
      int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0,
          GL11.GL_TEXTURE_HEIGHT);
      if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        textureCache.put(glTextureId, 0L);
        return 0;
      }
      ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
          GL11.GL_UNSIGNED_BYTE, pixels);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
      byte[] pixelData = new byte[width * height * 4];
      pixels.get(pixelData);
      long metalTex = NativeBridge.nCreateTexture2D(deviceHandle, width, height, pixelData);
      textureCache.put(glTextureId, metalTex);
      if (metalTex != 0 && frameCount < 10) {
        int nonWhite = 0;
        int nonBlack = 0;
        int transparent = 0;
        int fullyOpaque = 0;
        int totalPixels = pixelData.length / 4;
        int sampleCount = Math.min(256, totalPixels);
        for (int i = 0; i < sampleCount; i++) {
          int r = pixelData[i * 4] & 0xFF;
          int g = pixelData[i * 4 + 1] & 0xFF;
          int b = pixelData[i * 4 + 2] & 0xFF;
          int a = pixelData[i * 4 + 3] & 0xFF;
          if (r < 250 || g < 250 || b < 250)
            nonWhite++;
          if (r > 5 || g > 5 || b > 5)
            nonBlack++;
          if (a < 128)
            transparent++;
          if (a == 255)
            fullyOpaque++;
        }
        StringBuilder firstPixels = new StringBuilder();
        for (int i = 0; i < Math.min(4, totalPixels); i++) {
          int r = pixelData[i * 4] & 0xFF;
          int g = pixelData[i * 4 + 1] & 0xFF;
          int b = pixelData[i * 4 + 2] & 0xFF;
          int a = pixelData[i * 4 + 3] & 0xFF;
          firstPixels.append(String.format("[%d,%d,%d,%d]", r, g, b, a));
        }
        MetalLogger.info(
            "Entity texture: glId=%d size=%dx%d metal=%d " +
                "of%d: nonWhite=%d nonBlack=%d transparent=%d opaque=%d px:%s",
            glTextureId, width, height, metalTex, sampleCount, nonWhite,
            nonBlack, transparent, fullyOpaque, firstPixels.toString());
      }
      return metalTex;
    } catch (Exception e) {
      MetalLogger.error("Failed to create Metal entity texture for glId=%d: %s",
          glTextureId, e.getMessage());
      textureCache.put(glTextureId, 0L);
      return 0;
    }
  }
  public void invalidateTextureCache() {
    textureCache.clear();
    MetalLogger.info("Entity texture cache invalidated");
  }
  public int getLastEntityCount() {
    return pendingDraws.size();
  }
  public int getLastVertexCount() {
    return currentVertexCount;
  }
  public void shutdown() {
    active = false;
    capturedEntities.clear();
    pendingDraws.clear();
    textureCache.clear();
    if (metalVertexBuffer != 0) {
      NativeBridge.nDestroyBuffer(metalVertexBuffer);
      metalVertexBuffer = 0;
    }
    deviceHandle = 0;
    MetalLogger.info("MetalEntityRenderer shut down");
  }
  private static class CapturedEntity {
    Entity entity;
    float tickDelta;
    Matrix4f modelMatrix;
    boolean isHurt;
    float hurtFactor;
    int glTextureId;
  }
  private static class EntityDrawCommand {
    int startVertex;
    int vertexCount;
    float hurtFactor;
    float whiteFlash;
    int renderFlags;
    int glTextureId;
  }
}
