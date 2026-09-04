package com.pebbles_boon.metalrender.backend.metal;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.TransientMemory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class MetalTransientMemory implements TransientMemory {
  private final List<MetalGpuBuffer> allocations = new ArrayList<>();

  @Override
  public ByteBuffer allocateCpu(long size, long alignment, long frameIndex,
      long lifetime) {
    return ByteBuffer.allocateDirect(exactInt(size));
  }

  @Override
  public GpuBufferSlice.MappedView allocateStaging(long size, long alignment,
      int usage, long frameIndex, long lifetime) {
    MetalGpuBuffer buffer = allocateBuffer(size,
        usage | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC);
    return buffer.map(false, true);
  }

  @Override
  public GpuBufferSlice allocateGpu(long size, long alignment, int usage,
      long frameIndex, long lifetime) {
    return allocateBuffer(size, usage).slice();
  }

  @Override
  public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment,
      int usage, long frameIndex, long lifetime) {
    MetalGpuBuffer buffer = allocateBuffer(size,
        usage | GpuBuffer.USAGE_MAP_WRITE);
    return buffer.map(false, true);
  }

  @Override
  public GpuBufferSlice uploadStaging(List<ByteBuffer> sources, long alignment,
      int usage, long frameIndex, long lifetime) {
    return upload(sources,
        usage | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC);
  }

  @Override
  public GpuBufferSlice uploadGpu(List<ByteBuffer> sources, long alignment,
      int usage, long frameIndex, long lifetime) {
    return upload(sources, usage | GpuBuffer.USAGE_MAP_WRITE);
  }

  @Override
  public List<GpuBufferSlice> multiUploadStaging(List<ByteBuffer> sources,
      long alignment, int usage) {
    return multiUpload(sources,
        usage | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC);
  }

  @Override
  public List<GpuBufferSlice> multiUploadGpu(List<ByteBuffer> sources,
      long alignment, int usage) {
    return multiUpload(sources, usage | GpuBuffer.USAGE_MAP_WRITE);
  }

  void close() {
    for (MetalGpuBuffer allocation : allocations) {
      allocation.close();
    }
    allocations.clear();
  }

  private MetalGpuBuffer allocateBuffer(long size, int usage) {
    MetalGpuBuffer buffer = new MetalGpuBuffer(usage, size);
    allocations.add(buffer);
    return buffer;
  }

  private GpuBufferSlice upload(List<ByteBuffer> sources, int usage) {
    long size = totalRemaining(sources);
    MetalGpuBuffer buffer = allocateBuffer(size, usage);
    try (var mapped = buffer.map(false, true)) {
      for (ByteBuffer source : sources) {
        mapped.data().put(source.duplicate());
      }
    }
    return buffer.slice();
  }

  private List<GpuBufferSlice> multiUpload(List<ByteBuffer> sources,
      int usage) {
    List<GpuBufferSlice> result = new ArrayList<>(sources.size());
    for (ByteBuffer source : sources) {
      result.add(upload(List.of(source), usage));
    }
    return result;
  }

  private static long totalRemaining(List<ByteBuffer> sources) {
    long total = 0;
    for (ByteBuffer source : sources) {
      total = Math.addExact(total, source.remaining());
    }
    if (total == 0) {
      throw new IllegalArgumentException("Cannot upload empty transient data");
    }
    return total;
  }

  private static int exactInt(long size) {
    if (size < 0 || size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("CPU allocation is too large: " + size);
    }
    return (int)size;
  }
}
