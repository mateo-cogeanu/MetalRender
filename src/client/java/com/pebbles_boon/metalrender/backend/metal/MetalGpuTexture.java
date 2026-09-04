package com.pebbles_boon.metalrender.backend.metal;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;

public final class MetalGpuTexture extends GpuTexture {
  private long handle;

  public MetalGpuTexture(int usage, String label, GpuFormat format, int width,
      int height, int depthOrLayers, int mipLevels) {
    super(usage, label, format, width, height, depthOrLayers, mipLevels);
    handle = NativeBridge.nCreateBackendTexture(format.ordinal(), usage, width,
        height, depthOrLayers, mipLevels);
    if (handle == 0) {
      throw new IllegalStateException("Metal does not support texture format "
          + format + " for " + label);
    }
  }

  public long handle() {
    return handle;
  }

  @Override
  public void close() {
    if (handle != 0) {
      NativeBridge.nDestroyBackendTexture(handle);
      handle = 0;
    }
  }

  @Override
  public boolean isClosed() {
    return handle == 0;
  }
}
