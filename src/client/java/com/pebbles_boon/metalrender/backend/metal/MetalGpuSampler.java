package com.pebbles_boon.metalrender.backend.metal;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;
import java.util.OptionalDouble;

public final class MetalGpuSampler extends GpuSampler {
  private final AddressMode addressModeU;
  private final AddressMode addressModeV;
  private final FilterMode minFilter;
  private final FilterMode magFilter;
  private final int maxAnisotropy;
  private final OptionalDouble maxLod;
  private long handle;

  public MetalGpuSampler(AddressMode addressModeU, AddressMode addressModeV,
      FilterMode minFilter, FilterMode magFilter, int maxAnisotropy,
      OptionalDouble maxLod) {
    this.addressModeU = addressModeU;
    this.addressModeV = addressModeV;
    this.minFilter = minFilter;
    this.magFilter = magFilter;
    this.maxAnisotropy = maxAnisotropy;
    this.maxLod = maxLod;
    handle = NativeBridge.nCreateBackendSampler(addressModeU.ordinal(),
        addressModeV.ordinal(), minFilter.ordinal(), magFilter.ordinal(),
        maxAnisotropy, maxLod.orElse(-1.0));
    if (handle == 0) {
      throw new IllegalStateException("Metal failed to create a sampler");
    }
  }

  public long handle() {
    return handle;
  }

  @Override
  public AddressMode getAddressModeU() {
    return addressModeU;
  }

  @Override
  public AddressMode getAddressModeV() {
    return addressModeV;
  }

  @Override
  public FilterMode getMinFilter() {
    return minFilter;
  }

  @Override
  public FilterMode getMagFilter() {
    return magFilter;
  }

  @Override
  public int getMaxAnisotropy() {
    return maxAnisotropy;
  }

  @Override
  public OptionalDouble getMaxLod() {
    return maxLod;
  }

  @Override
  public void close() {
    if (handle != 0) {
      NativeBridge.nDestroyBackendSampler(handle);
      handle = 0;
    }
  }
}
