package com.pebbles_boon.metalrender.backend;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.pebbles_boon.metalrender.nativebridge.NativeBridge;
import com.pebbles_boon.metalrender.util.MetalLogger;
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
    } catch (BackendCreationException exception) {
      throw exception;
    } catch (Throwable throwable) {
      throw unavailable("Metal window bootstrap failed: " + throwable);
    }

    MetalLogger.info("Full-window Metal bootstrap passed with GLFW_NO_API; "
        + "falling back until the GpuDevice implementation is complete");
    throw unavailable("Metal window bootstrap passed; native GpuDevice work "
        + "is still in progress");
  }

  private static BackendCreationException unavailable(String message) {
    return new BackendCreationException(message,
        BackendCreationException.Reason.OTHER);
  }
}
