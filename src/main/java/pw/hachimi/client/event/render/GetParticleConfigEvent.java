package pw.hachimi.client.event.render;

import net.minecraft.world.attribute.AmbientParticle;
import pw.hachimi.client.api.UnidentifiedInterface2;
import pw.hachimi.client.event.Event;

@UnidentifiedInterface2
public class GetParticleConfigEvent extends Event {
   private AmbientParticle biomeParticleConfig;
   public static boolean fa;

   public AmbientParticle getAmbientParticle() {
      return this.biomeParticleConfig;
   }

   public void setAmbientParticle(AmbientParticle biomeParticleConfig2) {
      this.biomeParticleConfig = biomeParticleConfig2;
   }
}
