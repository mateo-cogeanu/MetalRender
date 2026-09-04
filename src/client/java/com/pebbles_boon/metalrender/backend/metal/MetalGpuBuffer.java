package com.pebbles_boon.metalrender.backend.metal;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;
import java.nio.ByteBuffer;

public final class MetalGpuBuffer extends GpuBuffer {
  private long handle;

  public MetalGpuBuffer(int usage, long size) {
    super(usage, size);
    handle = NativeBridge.nCreateBackendBuffer(size);
    if (handle == 0) {
      throw new IllegalStateException("Metal failed to allocate a " + size
          + " byte buffer");
    }
  }

  public long handle() {
    return handle;
  }

  @Override
  public boolean isClosed() {
    return handle == 0;
  }

  @Override
  public void close() {
    if (handle != 0) {
      NativeBridge.nDestroyBackendBuffer(handle);
      handle = 0;
    }
  }

  @Override
  public GpuBufferSlice.MappedView map(long offset, long length,
      boolean read, boolean write) {
    if (isClosed()) {
      throw new IllegalStateException("Cannot map a closed Metal buffer");
    }
    if (offset < 0 || length < 0 || offset > size() - length) {
      throw new IllegalArgumentException("Metal buffer map is out of bounds");
    }
    ByteBuffer data = NativeBridge.nMapBackendBuffer(handle, offset, length);
    if (data == null) {
      throw new IllegalStateException("Metal buffer mapping failed");
    }
    return new GpuBufferSlice.MappedView(
        new GpuBufferSlice(this, offset, length), data, () -> { });
  }
}
