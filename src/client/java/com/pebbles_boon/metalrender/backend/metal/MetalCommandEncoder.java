package com.pebbles_boon.metalrender.backend.metal;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;
import java.nio.ByteBuffer;
import org.joml.Vector4fc;

public final class MetalCommandEncoder implements CommandEncoderBackend {
  private long handle;
  private final MetalTransientMemory transientMemory =
      new MetalTransientMemory();

  public MetalCommandEncoder() {
    handle = NativeBridge.nCreateBackendCommandBuffer();
    if (handle == 0) {
      throw new IllegalStateException("Metal failed to create a command buffer");
    }
  }

  @Override
  public void submit() {
    submit(false);
  }

  public void submitAndWait() {
    submit(true);
  }

  private void submit(boolean wait) {
    requireOpen();
    NativeBridge.nSubmitBackendCommandBuffer(handle, wait);
    handle = 0;
    transientMemory.close();
  }

  @Override
  public TransientMemory transientMemory() {
    return transientMemory;
  }

  @Override
  public RenderPassBackend createRenderPass(RenderPassDescriptor descriptor) {
    throw unsupported("render passes");
  }

  @Override
  public void submitRenderPass() {
    throw unsupported("render passes");
  }

  @Override
  public void clearColorTexture(GpuTexture texture, Vector4fc color) {
    MetalGpuTexture metalTexture = texture(texture);
    requireOpen();
    if (!NativeBridge.nCommandBufferClearColor(handle, metalTexture.handle(),
        color.x(), color.y(), color.z(), color.w())) {
      throw new IllegalStateException("Metal color clear failed");
    }
  }

  @Override
  public void clearColorAndDepthTextures(GpuTexture colorTexture,
      Vector4fc color, GpuTexture depthTexture, double depth) {
    clearColorAndDepthTextures(colorTexture, color, depthTexture, depth,
        0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0));
  }

  @Override
  public void clearColorAndDepthTextures(GpuTexture colorTexture,
      Vector4fc color, GpuTexture depthTexture, double depth, int x, int y,
      int width, int height) {
    requireFullTextureClear(colorTexture, x, y, width, height);
    MetalGpuTexture metalColor = texture(colorTexture);
    MetalGpuTexture metalDepth = texture(depthTexture);
    requireOpen();
    if (!NativeBridge.nCommandBufferClearColorAndDepth(handle,
        metalColor.handle(), color.x(), color.y(), color.z(), color.w(),
        metalDepth.handle(), depth)) {
      throw new IllegalStateException("Metal color/depth clear failed");
    }
  }

  @Override
  public void clearDepthTexture(GpuTexture texture, double depth) {
    MetalGpuTexture metalTexture = texture(texture);
    requireOpen();
    if (!NativeBridge.nCommandBufferClearDepth(handle, metalTexture.handle(),
        depth)) {
      throw new IllegalStateException("Metal depth clear failed");
    }
  }

  @Override
  public void writeToBuffer(GpuBufferSlice destination, ByteBuffer source) {
    MetalGpuBuffer buffer = buffer(destination.buffer());
    ByteBuffer direct = directCopy(source);
    if (direct.remaining() > destination.length()) {
      throw new IllegalArgumentException("Source exceeds Metal buffer slice");
    }
    if (!NativeBridge.nWriteBackendBuffer(buffer.handle(),
        destination.offset(), direct, direct.position(), direct.remaining())) {
      throw new IllegalStateException("Metal buffer write failed");
    }
  }

  @Override
  public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice destination) {
    if (source.length() > destination.length()) {
      throw new IllegalArgumentException("Source exceeds Metal buffer slice");
    }
    requireOpen();
    if (!NativeBridge.nCommandBufferCopyBuffer(handle,
        buffer(source.buffer()).handle(), source.offset(),
        buffer(destination.buffer()).handle(), destination.offset(),
        source.length())) {
      throw new IllegalStateException("Metal buffer copy failed");
    }
  }

  @Override
  public void writeToTexture(GpuTexture texture, ByteBuffer source,
      int mipLevel, int depthOrLayer, int x, int y, int width, int height) {
    throw unsupported("texture uploads");
  }

  @Override
  public void copyBufferToTexture(GpuBufferSlice source, int bytesPerRow,
      int rowsPerImage, int sourceOffset, int sourceLength, GpuTexture texture,
      int mipLevel, int depthOrLayer, int x, int y, int width, int height) {
    throw unsupported("buffer-to-texture copies");
  }

  @Override
  public void copyTextureToBuffer(GpuTexture texture, GpuBuffer buffer,
      long offset, Runnable callback, int mipLevel) {
    throw unsupported("texture-to-buffer copies");
  }

  @Override
  public void copyTextureToBuffer(GpuTexture texture, GpuBuffer buffer,
      long offset, Runnable callback, int mipLevel, int x, int y, int width,
      int height) {
    throw unsupported("texture-to-buffer copies");
  }

  @Override
  public void copyTextureToTexture(GpuTexture source, GpuTexture destination,
      int mipLevel, int x, int y, int width, int height, int depthOrLayer,
      int destinationDepthOrLayer) {
    throw unsupported("texture-to-texture copies");
  }

  @Override
  public GpuFence createFence() {
    throw unsupported("GPU fences");
  }

  @Override
  public void writeTimestamp(GpuQueryPool queryPool, int index) {
    throw unsupported("timestamp queries");
  }

  private void requireOpen() {
    if (handle == 0) {
      throw new IllegalStateException("Metal command buffer was submitted");
    }
  }

  private static MetalGpuBuffer buffer(GpuBuffer buffer) {
    if (!(buffer instanceof MetalGpuBuffer metal) || metal.isClosed()) {
      throw new IllegalArgumentException("Expected an open Metal buffer");
    }
    return metal;
  }

  private static MetalGpuTexture texture(GpuTexture texture) {
    if (!(texture instanceof MetalGpuTexture metal) || metal.isClosed()) {
      throw new IllegalArgumentException("Expected an open Metal texture");
    }
    return metal;
  }

  private static ByteBuffer directCopy(ByteBuffer source) {
    ByteBuffer view = source.duplicate();
    if (view.isDirect()) {
      return view;
    }
    ByteBuffer direct = ByteBuffer.allocateDirect(view.remaining());
    direct.put(view).flip();
    return direct;
  }

  private static void requireFullTextureClear(GpuTexture texture, int x, int y,
      int width, int height) {
    if (x != 0 || y != 0 || width != texture.getWidth(0)
        || height != texture.getHeight(0)) {
      throw unsupported("partial texture clears");
    }
  }

  private static UnsupportedOperationException unsupported(String operation) {
    return new UnsupportedOperationException(
        "Metal backend does not implement " + operation + " yet");
  }
}
