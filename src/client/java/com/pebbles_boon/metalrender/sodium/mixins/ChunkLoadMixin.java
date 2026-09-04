package com.pebbles_boon.metalrender.sodium.mixins;
import com.pebbles_boon.metalrender.MetalRenderClient;
import com.pebbles_boon.metalrender.render.MetalWorldRenderer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;
import java.util.function.Consumer;
@Mixin(ClientChunkCache.class)
public class ChunkLoadMixin {
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"), require = 0)
    private void metalrender$onChunkLoaded(int x, int z, FriendlyByteBuf buf,
            Map<?, ?> blockEntityTags, Consumer<?> lightUpdateConsumer,
            CallbackInfoReturnable<LevelChunk> cir) {
        if (!MetalRenderClient.isEnabled())
            return;
        MetalWorldRenderer wr = MetalWorldRenderer.getInstance();
        if (wr == null || !wr.isReady())
            return;
        LevelChunk chunk = cir.getReturnValue();
        if (chunk != null) {
            wr.onChunkLoaded(x, z, chunk);
        }
    }
}
