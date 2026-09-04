package com.pebbles_boon.metalrender.backend.metal;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;

public final class MetalGpuTextureView extends GpuTextureView {
  private long handle;

  public MetalGpuTextureView(MetalGpuTexture texture, int baseMipLevel,
      int mipLevels) {
    super(texture, baseMipLevel, mipLevels);
    handle = NativeBridge.nCreateBackendTextureView(texture.handle(),
        baseMipLevel, mipLevels);
    if (handle == 0) {
      throw new IllegalStateException("Metal failed to create a texture view");
    }
  }

  public long handle() {
    return handle;
  }

  @Override
  public void close() {
    if (handle != 0) {
      NativeBridge.nDestroyBackendTextureView(handle);
      handle = 0;
    }
  }

  @Override
  public boolean isClosed() {
    return handle == 0;
  }
}
