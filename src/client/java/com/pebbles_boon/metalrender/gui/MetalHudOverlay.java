package com.pebbles_boon.metalrender.gui;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.backend.MetalGpuBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pebbles_boon.metalrender.nativebridge.MetalHardwareChecker;
import com.pebbles_boon.metalrender.nativebridge.DirectMetalPresentationProbe;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
@SuppressWarnings("deprecation")
public final class MetalHudOverlay {
  private static final int COLOR_OK = 0xFF55FF55;
  private static final int COLOR_WARN = 0xFFFFFF55;
  private static final int COLOR_INFO = 0xFFFFFFFF;
  private static final int BACKGROUND = 0xB0000000;

  public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
    Minecraft minecraft = Minecraft.getInstance();
    boolean debugScreen = minecraft.getDebugOverlay().showDebugScreen();
    boolean configured = MetalRenderClient.getConfig() != null &&
        MetalRenderClient.getConfig().enableDebugOverlay;
    if (!debugScreen && !configured) {
      return;
    }
    var font = minecraft.font;
    MetalWorldRenderer renderer = MetalRenderClient.getWorldRenderer();
    String backendName = "unavailable";
    String driverInfo = "";
    try {
      var deviceInfo = RenderSystem.getDevice().getDeviceInfo();
      backendName = deviceInfo.backendName();
      driverInfo = deviceInfo.driverInfo();
    } catch (Throwable ignored) {
    }
    boolean moltenVkActive = MetalGpuBackend.isRequested()
        && "Vulkan".equals(backendName) && driverInfo.contains("MoltenVK");
    boolean initialized = MetalRenderClient.isEnabled() && renderer != null &&
        renderer.isReady();
    boolean verified = initialized && renderer.getMetalFramesSubmitted() > 0 &&
        renderer.getSuccessfulCompositeCount() > 0;
    boolean activeOutput = moltenVkActive || verified;
    List<String> lines = new ArrayList<>();
    lines.add(moltenVkActive ? "MetalRender: ACTIVE VIA MOLTENVK"
        : verified ? "MetalRender: VERIFIED NATIVE METAL FRAMES"
                       : initialized ? "MetalRender: initialized; no verified output"
                                     : "MetalRender: inactive");
    lines.add("Device: " + MetalHardwareChecker.getDeviceName());
    if (renderer != null) {
      lines.add(String.format("Metal frames: %d  context: %s",
          renderer.getMetalFramesSubmitted(),
          renderer.didLastFrameHaveMetalContext() ? "yes" : "no"));
      lines.add(String.format("Metal chunks: %d now / %d total",
          renderer.getLastDrawnChunkCount(), renderer.getTotalMetalChunksDrawn()));
      lines.add(String.format("Present: %s  ok:%d fail:%d",
          renderer.getLastCompositePath(), renderer.getSuccessfulCompositeCount(),
          renderer.getFailedCompositeCount()));
      lines.add(String.format("Composite: fast:%d slow:%d  atlas fallback:%s",
          renderer.getFastPathCompositeCount(), renderer.getSlowPathCompositeCount(),
          renderer.getTextureManager().isUsingFallbackBlockAtlas() ? "yes" : "no"));
    }
    lines.add("Window backend: " + backendName
        + (moltenVkActive ? " (MoltenVK -> Metal)" : ""));
    if (DirectMetalPresentationProbe.isRequested()) {
      lines.add(String.format("Direct CAMetalLayer probe: %s  frames:%d",
          DirectMetalPresentationProbe.isActive() ? "active" : "failed",
          DirectMetalPresentationProbe.getFramesPresented()));
    }
    int width = 0;
    for (String line : lines)
      width = Math.max(width, font.width(line));
    int lineHeight = 10;
    int x = Math.max(4, context.guiWidth() - width - 8);
    int y = Math.max(4, context.guiHeight() - lines.size() * lineHeight - 36);
    context.fill(x - 3, y - 3, x + width + 3,
        y + lines.size() * lineHeight + 2, BACKGROUND);
    for (int i = 0; i < lines.size(); i++) {
      int color = i == 0 ? (activeOutput ? COLOR_OK : COLOR_WARN) : COLOR_INFO;
      context.text(font, lines.get(i), x, y + i * lineHeight, color, true);
    }
  }
  public static void register() {
    try {
      // Try new HudElementRegistry API first (MC 26.1+)
      net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(
          net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
          Identifier.fromNamespaceAndPath("metalrender", "hud"),
          MetalHudOverlay::onHudRender);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
