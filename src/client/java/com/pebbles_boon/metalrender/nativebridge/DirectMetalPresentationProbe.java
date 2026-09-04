package com.pebbles_boon.metalrender.nativebridge;

import com.pebbles_boon.metalrender.util.MetalLogger;
import org.lwjgl.glfw.GLFWNativeCocoa;

/**
 * Opt-in proof that Metal can present directly to Minecraft's Cocoa window.
 * This is deliberately separate from the hybrid IOSurface compositor.
 */
public final class DirectMetalPresentationProbe {
  private static final boolean REQUESTED = Boolean.getBoolean(
      "metalrender.experimental.directPresentationProbe");
  private static boolean active;
  private static long framesPresented;

  private DirectMetalPresentationProbe() {
  }

  public static void initialize(long glfwWindow) {
    if (!REQUESTED || active || glfwWindow == 0)
      return;
    try {
      long cocoaView = GLFWNativeCocoa.glfwGetCocoaView(glfwWindow);
      active = cocoaView != 0 &&
          NativeBridge.nCreateDirectPresentationProbe(cocoaView);
      MetalLogger.info("Direct Metal presentation probe: %s",
          active ? "ACTIVE" : "FAILED");
    } catch (Throwable t) {
      active = false;
      MetalLogger.error("Direct Metal presentation probe failed: %s",
          t.toString());
    }
  }

  public static void present() {
    if (!active)
      return;
    float pulse = 0.65f + 0.25f *
        (float)Math.sin(framesPresented * 0.045);
    if (NativeBridge.nPresentDirectPresentationProbe(0.05f, pulse, 0.18f)) {
      framesPresented++;
      if (framesPresented == 1 || framesPresented % 600 == 0) {
        MetalLogger.info("Direct CAMetalLayer presented frame %d",
            framesPresented);
      }
    }
  }

  public static boolean isRequested() {
    return REQUESTED;
  }

  public static boolean isActive() {
    return active;
  }

  public static long getFramesPresented() {
    return framesPresented;
  }

  public static void shutdown() {
    if (active)
      NativeBridge.nDestroyDirectPresentationProbe();
    active = false;
  }
}
