package pw.hachimi.client.mixin;

import net.minecraft.client.render.entity.FireworkRocketEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderEvent15;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(FireworkRocketEntityRenderer.class)
public class FireworkRocketEntityRendererMixin {
   @Inject(method = "render(Lnet/minecraft/client/render/entity/state/FireworkRocketEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
   private void hookRender(net.minecraft.client.render.entity.state.FireworkRocketEntityRenderState state, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo callbackInfo) {
      RenderEvent15 renderEvent15 = new RenderEvent15();
      EventBusImpl.INSTANCE.rC(renderEvent15);
      if (renderEvent15.aik()) {
         callbackInfo.cancel();
      }
   }
}
