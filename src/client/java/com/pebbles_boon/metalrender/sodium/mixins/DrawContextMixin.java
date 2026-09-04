package com.pebbles_boon.metalrender.sodium.mixins;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.render.gui.MetalGuiRenderer;
import com.pebbles_boon.metalrender.render.text.MetalTextRenderer;
import com.pebbles_boon.metalrender.sodium.mixins.accessor.SpriteAtlasTextureAccessor;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {
  private static final boolean CAPTURE_NON_TEXT_GUI = Boolean.parseBoolean(
      System.getProperty("metalrender.captureNonTextGui", "false"));
  private static int interceptCount = 0;
  private static long lastLogTime = 0;
  private static final Identifier GUI_ATLAS_ID =
      Identifier.fromNamespaceAndPath("minecraft", "textures/atlas/gui.png");
  private Vector2f transformPoint(float x, float y) {
    return new Vector2f(x, y);
  }
  private boolean shouldIntercept() {
    if (!MetalRenderClient.isEnabled()) {
      return false;
    }
    if (!MetalGuiRenderer.isOverlayModeEnabled()) {
      return false;
    }
    MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
    return gui.isEnabled() && gui.isFrameStarted();
  }
  @Inject(method = "drawTexture(Lcom/mojang/blaze3d/pipeline/"
                   +
                   "RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawTexture_9(RenderPipeline pipeline, Identifier texture, int x,
                            int y, float u, float v, int width, int height,
                            int texWidth, int texHeight, int z,
                            CallbackInfo ci) {
    if (intercept("tex9", texture, x, y, width, height, u, v, (float)width,
                  (float)height, texWidth, texHeight, 0xFFFFFFFF, z)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawTexture(Lcom/mojang/blaze3d/pipeline/"
                   + "RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawTexture_8(RenderPipeline pipeline, Identifier texture, int x,
                            int y, float u, float v, int width, int height,
                            int texWidth, int texHeight, CallbackInfo ci) {
    if (intercept("tex8", texture, x, y, width, height, u, v, (float)width,
                  (float)height, texWidth, texHeight, 0xFFFFFFFF, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawTexture(Lcom/mojang/blaze3d/pipeline/"
                   +
                   "RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawTexture_10(RenderPipeline pipeline, Identifier texture, int x,
                             int y, float u, float v, int width, int height,
                             int regionW, int regionH, int texWidth,
                             int texHeight, CallbackInfo ci) {
    if (intercept("tex10", texture, x, y, width, height, u, v, (float)regionW,
                  (float)regionH, texWidth, texHeight, 0xFFFFFFFF, 0)) {
      ci.cancel();
    }
  }
  @Inject(
      method = "drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/"
               + "minecraft/resources/Identifier;IIFFIIIIIII)V",
      at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawTexture_11(RenderPipeline pipeline, Identifier texture, int x,
                             int y, float u, float v, int width, int height,
                             int regionW, int regionH, int texWidth,
                             int texHeight, int z, CallbackInfo ci) {
    if (intercept("tex11", texture, x, y, width, height, u, v, (float)regionW,
                  (float)regionH, texWidth, texHeight, 0xFFFFFFFF, z)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawGuiTexture(Lcom/mojang/blaze3d/pipeline/"
                   + "RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawGuiTexture_IIII(RenderPipeline pipeline, Identifier spriteId,
                                  int x, int y, int width, int height,
                                  CallbackInfo ci) {
    if (interceptGuiSprite("gui_IIII", spriteId, x, y, width, height,
                           0xFFFFFFFF, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawGuiTexture(Lcom/mojang/blaze3d/pipeline/"
                   + "RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIF)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawGuiTexture_IIIIF(RenderPipeline pipeline, Identifier spriteId,
                                   int x, int y, int width, int height,
                                   float alpha, CallbackInfo ci) {
    int color = ((int)(alpha * 255) << 24) | 0xFFFFFF;
    if (interceptGuiSprite("gui_IIIIF", spriteId, x, y, width, height, color,
                           0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawGuiTexture(Lcom/mojang/blaze3d/pipeline/"
                   + "RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawGuiTexture_IIIII(RenderPipeline pipeline, Identifier spriteId,
                                   int x, int y, int width, int height, int z,
                                   CallbackInfo ci) {
    if (interceptGuiSprite("gui_IIIII", spriteId, x, y, width, height,
                           0xFFFFFFFF, z)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawGuiTexture(Lcom/mojang/blaze3d/pipeline/"
                   + "RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawGuiTexture_IIIIIIII(RenderPipeline pipeline,
                                      Identifier spriteId, int x, int y,
                                      int width, int height, int u, int v,
                                      int texW, int texH, CallbackInfo ci) {
    if (interceptGuiSpriteWithUV("gui_IIIIIIII", spriteId, x, y, width, height,
                                 u, v, texW, texH, 0xFFFFFFFF, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawGuiTexture(Lcom/mojang/blaze3d/pipeline/"
                   +
                   "RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawGuiTexture_IIIIIIIII(RenderPipeline pipeline,
                                       Identifier spriteId, int x, int y,
                                       int width, int height, int u, int v,
                                       int texW, int texH, int z,
                                       CallbackInfo ci) {
    if (interceptGuiSpriteWithUV("gui_IIIIIIIII", spriteId, x, y, width, height,
                                 u, v, texW, texH, 0xFFFFFFFF, z)) {
      ci.cancel();
    }
  }
  private static int itemInterceptCount = 0;
  private static long lastItemLogTime = 0;
  private static int lastLoggedFrame = 0;
  private static void resetItemCounterIfNewFrame() {
    int currentFrame =
        net.minecraft.client.Minecraft.getInstance().gui.hud.getGuiTicks();
    if (currentFrame != lastLoggedFrame) {
      lastLoggedFrame = currentFrame;
      itemInterceptCount = 0;
    }
  }
  @Inject(method = "drawItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/"
                   + "minecraft/world/item/ItemStack;III)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawItem_EntityXYZ(LivingEntity entity, ItemStack stack, int x,
                                 int y, int z, CallbackInfo ci) {
    if (interceptItem("drawItem_EntityXYZ", stack, x, y, z)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/"
                   +
                   "minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawItem_EntityWorldXYZ(LivingEntity entity, net.minecraft.world.level.Level world,
                                      ItemStack stack, int x, int y, int z,
                                      CallbackInfo ci) {
    if (interceptItem("drawItem_EntityWorldXYZ", stack, x, y, z)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawItem(Lnet/minecraft/world/item/ItemStack;II)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawItem_XY(ItemStack stack, int x, int y, CallbackInfo ci) {
    if (interceptItem("drawItem_XY", stack, x, y, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawItem(Lnet/minecraft/world/item/ItemStack;III)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawItem_XYZ(ItemStack stack, int x, int y, int z,
                           CallbackInfo ci) {
    if (interceptItem("drawItem_XYZ", stack, x, y, z)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawItemWithoutEntity(Lnet/minecraft/world/item/ItemStack;II)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawItemWithoutEntity_XY(ItemStack stack, int x, int y,
                                       CallbackInfo ci) {
    if (interceptItem("drawItemWithoutEntity_XY", stack, x, y, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawItemWithoutEntity(Lnet/minecraft/world/item/ItemStack;III)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawItemWithoutEntity_XYZ(ItemStack stack, int x, int y, int z,
                                        CallbackInfo ci) {
    if (interceptItem("drawItemWithoutEntity_XYZ", stack, x, y, z)) {
      ci.cancel();
    }
  }
  private static int itemDebugCount = 0;
  private static final Identifier BLOCKS_ATLAS_ID =
      Identifier.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
  private static final Identifier ITEMS_ATLAS_ID =
      Identifier.fromNamespaceAndPath("minecraft", "textures/atlas/items.png");
  private Identifier lastSpriteAtlas = null;
  private static int spriteLookupDebugCount = 0;
  private static boolean loggedAllSprites = false;
  private TextureAtlasSprite lookupItemSprite(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    lastSpriteAtlas = null;
    try {
      Minecraft mc = Minecraft.getInstance();
      Identifier itemId =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      AbstractTexture itemsTexture =
          mc.getTextureManager().getTexture(ITEMS_ATLAS_ID);
      if (itemsTexture instanceof TextureAtlas itemsAtlas) {
        SpriteAtlasTextureAccessor accessor =
            (SpriteAtlasTextureAccessor)itemsAtlas;
        Map<Identifier, TextureAtlasSprite> sprites = accessor.metalrender$getSprites();
        if (sprites != null) {
          if (!loggedAllSprites) {
            loggedAllSprites = true;
          }
          Identifier spriteId =
              Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
          TextureAtlasSprite sprite = sprites.get(spriteId);
          if (sprite != null) {
            lastSpriteAtlas = ITEMS_ATLAS_ID;
            return sprite;
          }
        }
      }
      AbstractTexture blocksTexture =
          mc.getTextureManager().getTexture(BLOCKS_ATLAS_ID);
      if (blocksTexture instanceof TextureAtlas blocksAtlas) {
        SpriteAtlasTextureAccessor accessor =
            (SpriteAtlasTextureAccessor)blocksAtlas;
        Map<Identifier, TextureAtlasSprite> sprites = accessor.metalrender$getSprites();
        if (sprites != null) {
          Identifier spriteId =
              Identifier.fromNamespaceAndPath(itemId.getNamespace(), "block/" + itemId.getPath());
          TextureAtlasSprite sprite = sprites.get(spriteId);
          if (sprite != null) {
            lastSpriteAtlas = BLOCKS_ATLAS_ID;
            return sprite;
          }
        }
      }
    } catch (Exception e) {
    }
    return null;
  }
  private boolean interceptItem(String source, ItemStack stack, int x, int y,
                                int z) {
    if (!CAPTURE_NON_TEXT_GUI) {
      return false;
    }
    if (!shouldIntercept() || stack == null || stack.isEmpty()) {
      return false;
    }
    try {
      MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
      if (!gui.isEnabled()) {
        return false;
      }
      int itemSize = 16;
      float screenX1 = x;
      float screenY1 = y;
      float screenX2 = x + itemSize;
      float screenY2 = y + itemSize;
      TextureAtlasSprite sprite = lookupItemSprite(stack);
      itemDebugCount++;
      Minecraft mc = Minecraft.getInstance();
      String screenName = mc.gui.screen() != null
                              ? mc.gui.screen().getClass().getSimpleName()
                              : "InGame";
      if (sprite != null && lastSpriteAtlas != null) {
        gui.setTexture(lastSpriteAtlas);
        float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();
        float guiZ = 100.0f;
        gui.addQuad(screenX1, screenY1, screenX2, screenY2, guiZ, u1, v1, u2,
                    v2, 0xFFFFFFFF);
      } else {
        return false;
      }
      return true;
    } catch (Exception e) {
      System.err.println("[GuiGraphicsMixin] Error rendering item via Metal: " +
                         e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
  @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true, require = 0)
  private void metalrender$fill_IIIII(int x1, int y1, int x2, int y2, int color,
                                      CallbackInfo ci) {
    if (interceptFill("fill_IIIII", x1, y1, x2, y2, color, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$fill_IIIII_pipeline(RenderPipeline pipeline, int x1, int y1,
                                  int x2, int y2, int color, CallbackInfo ci) {
    if (interceptFill("fill_P_IIIII", x1, y1, x2, y2, color, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "fillGradient(IIIIII)V", at = @At("HEAD"),
          cancellable = true, require = 0)
  private void
  metalrender$fillGradient_IIIIII(int x1, int y1, int x2, int y2, int colorTop,
                                  int colorBottom, CallbackInfo ci) {
    if (interceptFill("fillGrad", x1, y1, x2, y2, colorTop, 0)) {
      ci.cancel();
    }
  }
  @Inject(method = "drawSpriteStretched(Lcom/mojang/blaze3d/pipeline/"
                   +
                   "RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIII)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawSpriteStretched_IIII(RenderPipeline pipeline, TextureAtlasSprite sprite,
                                       int x, int y, int width, int height,
                                       CallbackInfo ci) {
    if (interceptSprite("sprite_IIII", x, y, 0, width, height, sprite,
                        0xFFFFFFFF)) {
      ci.cancel();
    }
  }
  @Inject(
      method = "drawSpriteStretched(Lcom/mojang/blaze3d/pipeline/"
               + "RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V",
      at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawSpriteStretched_IIIII(RenderPipeline pipeline, TextureAtlasSprite sprite,
                                        int x, int y, int width, int height,
                                        int color, CallbackInfo ci) {
    if (interceptSprite("sprite_IIIII", x, y, 0, width, height, sprite,
                        color)) {
      ci.cancel();
    }
  }
  private TextureAtlasSprite lookupGuiSprite(Identifier spriteId) {
    try {
      Minecraft mc = Minecraft.getInstance();
      AbstractTexture texture = mc.getTextureManager().getTexture(GUI_ATLAS_ID);
      if (texture instanceof TextureAtlas atlas) {
        SpriteAtlasTextureAccessor accessor = (SpriteAtlasTextureAccessor)atlas;
        Map<Identifier, TextureAtlasSprite> sprites = accessor.metalrender$getSprites();
        if (sprites != null) {
          return sprites.get(spriteId);
        }
      }
    } catch (Exception e) {
    }
    return null;
  }
  private static int spriteNotFoundCount = 0;
  private static long lastSpriteLogTime = 0;
  private static int guiSpriteLogCount = 0;
  private boolean interceptGuiSprite(String type, Identifier spriteId, int x,
                                     int y, int width, int height, int color,
                                     int z) {
    if (!CAPTURE_NON_TEXT_GUI) {
      return false;
    }
    if (!shouldIntercept())
      return false;
    MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
    if (!gui.isEnabled())
      return false;
    try {
      TextureAtlasSprite sprite = lookupGuiSprite(spriteId);
      if (sprite != null) {
        float screenX1 = x;
        float screenY1 = y;
        float screenX2 = x + width;
        float screenY2 = y + height;
        Identifier atlasId = sprite.atlasLocation();
        gui.setTexture(atlasId);
        float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();
        gui.addQuad(screenX1, screenY1, screenX2, screenY2, (float)z, u1, v1,
                    u2, v2, color);
      } else {
        spriteNotFoundCount++;
        long now = System.currentTimeMillis();
        if (now - lastSpriteLogTime > 5000 && spriteNotFoundCount < 100) {
          lastSpriteLogTime = now;
          com.pebbles_boon.metalrender.util.MetalLogger.warn(
              "[GuiGraphicsMixin] TextureAtlasSprite not found: {} (count={})", spriteId,
              spriteNotFoundCount);
        }
        return false;
      }
    } catch (Exception e) {
      com.pebbles_boon.metalrender.util.MetalLogger.warn(
          "[GuiGraphicsMixin] Exception resolving sprite {}: {}", spriteId,
          e.getMessage());
      return false;
    }
    interceptCount++;
    return true;
  }
  private boolean interceptGuiSpriteWithUV(String type, Identifier spriteId,
                                           int x, int y, int width, int height,
                                           int u, int v, int texW, int texH,
                                           int color, int z) {
    if (!CAPTURE_NON_TEXT_GUI) {
      return false;
    }
    if (!shouldIntercept())
      return false;
    MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
    if (!gui.isEnabled())
      return false;
    try {
      TextureAtlasSprite sprite = lookupGuiSprite(spriteId);
      if (sprite != null) {
        float screenX1 = x;
        float screenY1 = y;
        float screenX2 = x + width;
        float screenY2 = y + height;
        Identifier atlasId = sprite.atlasLocation();
        gui.setTexture(atlasId);
        float spriteU1 = sprite.getU0();
        float spriteV1 = sprite.getV0();
        float spriteURange = sprite.getU1() - sprite.getU0();
        float spriteVRange = sprite.getV1() - sprite.getV0();
        float u1 = spriteU1 + (spriteURange * u / texW);
        float v1 = spriteV1 + (spriteVRange * v / texH);
        float u2 = spriteU1 + (spriteURange * (u + width) / texW);
        float v2 = spriteV1 + (spriteVRange * (v + height) / texH);
        gui.addQuad(screenX1, screenY1, screenX2, screenY2, (float)z, u1, v1,
                    u2, v2, color);
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
    interceptCount++;
    return true;
  }
  private static int textureLogCount = 0;
  private boolean intercept(String type, Identifier texture, int x, int y,
                            int width, int height, float u, float v,
                            float regionW, float regionH, int texWidth,
                            int texHeight, int color, int z) {
    if (!CAPTURE_NON_TEXT_GUI) {
      return false;
    }
    if (!shouldIntercept())
      return false;
    MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
    if (!gui.isEnabled())
      return false;
    float screenX1 = x;
    float screenY1 = y;
    float screenX2 = x + width;
    float screenY2 = y + height;
    gui.setTexture(texture);
    float u1 = u / texWidth;
    float v1 = v / texHeight;
    float u2 = (u + regionW) / texWidth;
    float v2 = (v + regionH) / texHeight;
    gui.addQuad(screenX1, screenY1, screenX2, screenY2, (float)z, u1, v1, u2,
                v2, color);
    interceptCount++;
    return true;
  }
  private static int fillLogCount = 0;
  private boolean interceptFill(String type, int x1, int y1, int x2, int y2,
                                int color, int z) {
    if (!CAPTURE_NON_TEXT_GUI) {
      return false;
    }
    if (!shouldIntercept())
      return false;
    MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
    if (!gui.isEnabled())
      return false;
    float screenX1 = x1;
    float screenY1 = y1;
    float screenX2 = x2;
    float screenY2 = y2;
    int alpha = (color >> 24) & 0xFF;
    if (alpha == 0) {
      interceptCount++;
      return true;
    }
    gui.setTexture(null);
    gui.addQuad(screenX1, screenY1, screenX2, screenY2, (float)z, 0, 0, 1, 1,
                color);
    interceptCount++;
    return true;
  }
  private boolean interceptSprite(String type, int x, int y, int z, int width,
                                  int height, TextureAtlasSprite sprite, int color) {
    if (!CAPTURE_NON_TEXT_GUI) {
      return false;
    }
    if (!shouldIntercept())
      return false;
    MetalGuiRenderer gui = MetalGuiRenderer.getInstance();
    if (!gui.isEnabled())
      return false;
    float screenX1 = x;
    float screenY1 = y;
    float screenX2 = x + width;
    float screenY2 = y + height;
    Identifier atlasId = sprite.atlasLocation();
    gui.setTexture(atlasId);
    float u1 = sprite.getU0();
    float v1 = sprite.getV0();
    float u2 = sprite.getU1();
    float v2 = sprite.getV1();
    gui.addQuad(screenX1, screenY1, screenX2, screenY2, (float)z, u1, v1, u2,
                v2, color);
    interceptCount++;
    return true;
  }
  private static int textInterceptCount = 0;
  @Inject(method = "drawText(Lnet/minecraft/client/gui/Font;Ljava/"
                   + "lang/String;IIIZ)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawText_String(Font textRenderer, String text, int x,
                              int y, int color, boolean shadow,
                              CallbackInfo ci) {
    if (!shouldIntercept())
      return;
    MetalTextRenderer metalText = MetalTextRenderer.getInstance();
    if (!metalText.isEnabled())
      return;
    textInterceptCount++;
    Vector2f transformed = transformPoint(x, y);
    metalText.drawText(textRenderer, text, (int)transformed.x,
                       (int)transformed.y, color, shadow);
    ci.cancel();
  }
  @Inject(method = "drawText(Lnet/minecraft/client/gui/Font;Lnet/"
                   + "minecraft/network/chat/Component;IIIZ)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawText_Text(Font textRenderer, Component text, int x, int y,
                            int color, boolean shadow, CallbackInfo ci) {
    if (!shouldIntercept())
      return;
    MetalTextRenderer metalText = MetalTextRenderer.getInstance();
    if (!metalText.isEnabled())
      return;
    textInterceptCount++;
    Vector2f transformed = transformPoint(x, y);
    String str = text.getString();
    metalText.drawText(textRenderer, str, (int)transformed.x,
                       (int)transformed.y, color, shadow);
    ci.cancel();
  }
  @Inject(method = "drawText(Lnet/minecraft/client/gui/Font;Lnet/"
                   + "minecraft/util/FormattedCharSequence;IIIZ)V",
          at = @At("HEAD"), cancellable = true, require = 0)
  private void
  metalrender$drawText_OrderedText(Font textRenderer, FormattedCharSequence text,
                                   int x, int y, int color, boolean shadow,
                                   CallbackInfo ci) {
    if (!shouldIntercept())
      return;
    MetalTextRenderer metalText = MetalTextRenderer.getInstance();
    if (!metalText.isEnabled())
      return;
    textInterceptCount++;
    Vector2f transformed = transformPoint(x, y);
    metalText.drawOrderedText(textRenderer, text, (int)transformed.x,
                              (int)transformed.y, color, shadow);
    ci.cancel();
  }
}
