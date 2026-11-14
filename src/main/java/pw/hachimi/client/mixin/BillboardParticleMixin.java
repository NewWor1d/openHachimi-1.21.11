package pw.hachimi.client.mixin;

import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 1.21.11: setColor 从 Particle 下移到 BillboardParticle。 */
@Mixin(BillboardParticle.class)
public abstract class BillboardParticleMixin {
   @Shadow
   public abstract void setColor(float red, float green, float blue);
}
