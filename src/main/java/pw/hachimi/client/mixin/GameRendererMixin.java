package pw.hachimi.client.mixin;

import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererMixin {
   // 1.21.11: renderHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix)
   @Invoker("renderHand")
   void hookRenderHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix);
}
