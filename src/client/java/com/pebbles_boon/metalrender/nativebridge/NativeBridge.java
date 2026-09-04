package com.pebbles_boon.metalrender.nativebridge;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
public final class NativeBridge {
  private static volatile boolean libLoaded;
  static {
    libLoaded = tryLoadLibrary();
  }
  private NativeBridge() {
  }
  public static synchronized void loadLibrary() {
    if (libLoaded)
      return;
    libLoaded = tryLoadLibrary();
    if (!libLoaded)
      throw new UnsatisfiedLinkError(
          "Could not load libmetalrender.dylib from java.library.path or mod JAR");
  }
  private static boolean tryLoadLibrary() {
    try {
      System.loadLibrary("metalrender");
      return true;
    } catch (Throwable ignored) {
      // Installed mods keep their native library inside the JAR. Extract it to
      // a private temporary directory before asking the JVM to load it.
    }
    try (InputStream bundled =
             NativeBridge.class.getResourceAsStream("/libmetalrender.dylib")) {
      if (bundled == null)
        return false;
      Path nativeDir = Files.createTempDirectory("metalrender-native-");
      Path library = nativeDir.resolve("libmetalrender.dylib");
      Files.copy(bundled, library, StandardCopyOption.REPLACE_EXISTING);
      library.toFile().deleteOnExit();
      nativeDir.toFile().deleteOnExit();
      System.load(library.toAbsolutePath().toString());
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }
  public static boolean isLibLoaded() {
    return libLoaded;
  }
  public static native boolean nIsAvailable();
  public static native long nInit(int width, int height, float scale);
  public static native void nResize(long handle, int width, int height,
      float scale);
  public static native void nBeginFrame(long handle, float[] proj, float[] view,
      float fogStart, float fogEnd);
  public static native void nDrawTerrain(long handle, int layerId);
  public static native void nDrawOverlay(long handle, int layerId);
  public static native void nOnWorldLoaded(long handle);
  public static native void nOnWorldUnloaded(long handle);
  public static native void nDestroy(long handle);
  public static native String nGetDeviceName();
  public static native boolean nSupportsIndirect();
  public static native boolean nSupportsMeshShaders();
  public static native long nCreateBuffer(long deviceHandle, int sizeBytes,
      int storageMode);
  public static native void nUploadBufferData(long bufferHandle, byte[] data,
      int offset, int length);
  public static native void nUploadBufferDataDirect(long bufferHandle,
      java.nio.ByteBuffer data,
      int offset, int length);
  public static native void nDestroyBuffer(long bufferHandle);
  public static native void nSetPipelineState(long frameContext,
      long pipelineHandle);
  public static native void nSetChunkOffset(long frameContext, float x, float y,
      float z);
  public static native void nDrawIndexedBuffer(long frameContext,
      long vertexBuffer,
      long indexBuffer, int indexCount,
      int baseIndex);
  public static native void nDrawIndexedBatch(long frameContext,
      long indexBuffer,
      float[] drawData, int drawCount);
  public static native void nDrawBuffer(long frameContext, long vertexBuffer,
      int vertexCount, int baseVertex);
  public static native long nGetCurrentFrameContext(long handle);
  public static native void nEndFrame(long handle);
  public static native void nSetProjectionMatrix(long handle, float[] matrix);
  public static native void nSetModelViewMatrix(long handle, float[] matrix);
  public static native void nSetCameraPosition(long handle, double x, double y,
      double z);
  public static native void nBindTexture(long handle, long textureHandle,
      int slot);
  public static native long nCreateTexture2D(long deviceHandle, int width,
      int height, byte[] pixelData);
  public static native void nUpdateTexture2D(long textureHandle, int width,
      int height, byte[] pixelData);
  public static native long nGetDeviceHandle(long handle);
  public static native long nGetShaderLibraryHandle(long handle);
  public static native long nGetInhousePipelineHandle(long handle);
  public static native long nGetDefaultPipelineHandle(long handle);
  public static native int nGetGLTextureId(long handle);
  public static native int nGetIOSurfaceWidth(long handle);
  public static native int nGetIOSurfaceHeight(long handle);
  public static native void nWaitForRender(long handle);
  public static native boolean nIsFrameReady(long handle);
  public static native void nSetReuseTerrainFrame(boolean reuse);
  public static native boolean nBindIOSurfaceToTexture(long handle,
      int glTexture);
  public static native boolean nReadbackPixels(long handle,
      java.nio.ByteBuffer dest);
  public static native boolean nReadbackDepth(long handle,
      java.nio.ByteBuffer dest);
  public static native long nGetEntityPipelineHandle(long handle);
  public static native long nGetEntityTranslucentPipelineHandle(long handle);
  public static native long nGetEntityEmissivePipelineHandle(long handle);
  public static native long nGetParticlePipelineHandle(long handle);
  public static native void nSetEntityOverlay(long frameContext, float hurtTime,
      float whiteFlash, float alpha);
  public static native void nSetEntityTintColor(long frameContext, float r,
      float g, float b, float a);
  public static native void nBindEntityTexture(long frameContext,
      long textureHandle);
  public static native void nDrawEntityBuffer(long frameContext,
      long vertexBuffer,
      int vertexCount, int baseVertex,
      int renderFlags);
  public static native void nDrawEntityBufferIndexed(long frameContext, long vertexBuffer,
      long indexBuffer, int indexCount, int baseIndex,
      int renderFlags);
  public static native void nSetDebugColor(long frameContext, float r, float g,
      float b, float a);
  public static native void nDrawLineBuffer(long frameContext,
      long vertexBuffer, int vertexCount);
  public static native void nDrawTriangleBuffer(long frameContext,
      long vertexBuffer, int vertexCount);
  public static native void nUploadCameraUniforms(long handle,
      float[] viewProj, float[] proj, float[] modelView,
      float[] cameraPos, float[] frustumPlanes,
      float screenW, float screenH, float nearPlane, float farPlane,
      int totalChunks);
  public static native void nUploadSubChunkData(long handle,
      java.nio.ByteBuffer directBuffer, int count);
  public static native void nUploadChunkUniforms(long handle,
      java.nio.ByteBuffer directBuffer, int count);
  public static native void nSetGPUDrivenEnabled(long handle, boolean enabled);
  public static native int nRunGPUCulling(long handle, int chunkCount);
  public static native void nExecuteIndirectDraws(long frameContext,
      long vertexBuffer, long indexBuffer);
  public static native int nGetGPUVisibleCount(long handle);
  public static native void nGetGPUCullStats(int[] outStats);
  public static native int nGetThermalState();
  public static native int nGetThermalLODReduction();
  public static native long nGetAvailableMemory();
  public static native int nGetHiZMipCount();
  public static native boolean nIsGPUDrivenActive();
  public static native boolean nAreMeshShadersActive();
  public static native boolean nCreateDirectPresentationProbe(long cocoaView);
  public static native boolean nPresentDirectPresentationProbe(float red,
      float green, float blue);
  public static native void nDestroyDirectPresentationProbe();
  public static native boolean nProbeMetalBackendWindow(long cocoaView);
  public static native long nCreateBackendBuffer(long size);
  public static native java.nio.ByteBuffer nMapBackendBuffer(long buffer,
      long offset, long length);
  public static native void nDestroyBackendBuffer(long buffer);
  public static native long nCreateBackendTexture(int format, int usage,
      int width, int height, int depthOrLayers, int mipLevels);
  public static native void nDestroyBackendTexture(long texture);
  public static native long nCreateBackendTextureView(long texture,
      int baseMipLevel, int mipLevels);
  public static native void nDestroyBackendTextureView(long textureView);
  public static native long nCreateBackendSampler(int addressModeU,
      int addressModeV, int minFilter, int magFilter, int maxAnisotropy,
      double maxLod);
  public static native void nDestroyBackendSampler(long sampler);
  public static native void nRegisterChunkMesh(int cx, int cy, int cz,
      long bufferHandle, int quadCount, int opaqueQuadCount, int lodLevel);
  public static native void nUnregisterChunkMesh(int cx, int cy, int cz);
  public static native int nDrawAllVisibleChunks(long frameContext,
      long indexBuffer);
}
