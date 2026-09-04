package com.pebbles_boon.metalrender.sodium.mixins.accessor;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ParticleEngine.class)
public interface ParticleManagerAccessor {
  // TODO: MC 26.1 changed the internal particle storage.
  // ParticleRenderer no longer exists. The particles field type has changed.
  // This accessor needs to be updated to match the new field type.
  // For now, use Object to allow compilation.
  @Accessor("particles")
  Map<ParticleRenderType, ?> metalrender$getParticles();
}
