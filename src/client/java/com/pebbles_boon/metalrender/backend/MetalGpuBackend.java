package com.pebbles_boon.metalrender.backend;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pebbles_boon.metalrender.backend.metal.MetalGpuBuffer;
import com.pebbles_boon.metalrender.backend.metal.MetalGpuSampler;
import com.pebbles_boon.metalrender.backend.metal.MetalGpuTexture;
import com.pebbles_boon.metalrender.backend.metal.MetalGpuTextureView;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;
import com.pebbles_boon.metalrender.util.MetalLogger;
import java.util.OptionalDouble;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;

/**
 * Bootstrap for the native Minecraft 26.2 GPU backend contract.
 *
 * <p>This backend currently proves that Minecraft can create its real window
 * without an OpenGL context and that Metal can present to that window. It then
 * fails creation deliberately so Minecraft can recreate the window using its
 * next configured backend. Returning a device before all resource and command
 * contracts are implemented would turn an expected fallback into a later
 * startup crash.</p>
 */
public final class MetalGpuBackend implements GpuBackend {
  public static final String ENABLE_PROPERTY =
      "metalrender.experimental.fullMetalBackend";

  @Override
  public String getName() {
    return "Metal (experimental bootstrap)";
  }

  @Override
  public void setWindowHints() {
    GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
  }

  @Override
  public void handleWindowCreationErrors(GLFWErrorCapture.Error error)
      throws BackendCreationException {
    String detail = error == null
        ? "GLFW could not create a no-API Metal window"
        : "GLFW could not create a no-API Metal window (error 0x"
            + Integer.toHexString(error.error()) + ")";
    throw new BackendCreationException(detail,
        BackendCreationException.Reason.GLFW_ERROR);
  }

  @Override
  public GpuDevice createDevice(long window, ShaderSource shaderSource,
      GpuDebugOptions debugOptions, Runnable criticalShaderLoader)
      throws BackendCreationException {
    if (!NativeBridge.isLibLoaded()) {
      throw unavailable("Bundled Metal native library could not be loaded");
    }

    try {
      long cocoaView = GLFWNativeCocoa.glfwGetCocoaView(window);
      if (cocoaView == 0 || !NativeBridge.nProbeMetalBackendWindow(cocoaView)) {
        throw unavailable("Metal could not present to Minecraft's no-API window");
      }
      validateResourceLayer();
    } catch (BackendCreationException exception) {
      throw exception;
    } catch (Throwable throwable) {
      throw unavailable("Metal window bootstrap failed: " + throwable);
    }

    MetalLogger.info("Full-window Metal bootstrap and resource self-test "
        + "passed with GLFW_NO_API; falling back until command encoding is "
        + "complete");
    throw unavailable("Metal window and resource bootstrap passed; native "
        + "command encoding is still in progress");
  }

  private static void validateResourceLayer() {
    try (MetalGpuBuffer buffer = new MetalGpuBuffer(
             GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC, 256);
         MetalGpuTexture texture = new MetalGpuTexture(
             GpuTexture.USAGE_TEXTURE_BINDING
                 | GpuTexture.USAGE_RENDER_ATTACHMENT,
             "Metal backend self-test", GpuFormat.RGBA8_UNORM, 4, 4, 1, 1);
         MetalGpuTextureView view = new MetalGpuTextureView(texture, 0, 1);
         MetalGpuSampler sampler = new MetalGpuSampler(
             AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
             FilterMode.NEAREST, FilterMode.NEAREST, 1,
             OptionalDouble.empty())) {
      try (var mapped = buffer.map(0, 16, false, true)) {
        mapped.data().putLong(0, 0x4d4554414c524553L);
        mapped.data().putLong(8, 0x4f55524345534f4bL);
      }
      if (view.handle() == 0 || sampler.handle() == 0) {
        throw new IllegalStateException("Metal resource handle was null");
      }
    }
  }

  private static BackendCreationException unavailable(String message) {
    return new BackendCreationException(message,
        BackendCreationException.Reason.OTHER);
  }
}
