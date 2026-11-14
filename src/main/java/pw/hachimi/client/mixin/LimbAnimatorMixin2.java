package pw.hachimi.client.mixin;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorMixin2 {
   @Accessor("animationProgress")
   void hookSetPos(float var1);

   @Accessor("animationProgress")
   float hookGetPos();

   @Accessor("speed")
   void hookSetSpeed(float var1);
}
