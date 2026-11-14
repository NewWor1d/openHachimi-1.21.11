package pw.hachimi.client.mixin;

import net.minecraft.client.render.entity.TridentEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderEvent12;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(TridentEntityRenderer.class)
public class TridentEntityRendererMixin {
   @Inject(method = "render(Lnet/minecraft/client/render/entity/state/TridentEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
   private void hookRender(net.minecraft.client.render.entity.state.TridentEntityRenderState state, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo callbackInfo) {
      RenderEvent12 renderEvent12 = new RenderEvent12();
      EventBusImpl.INSTANCE.rC(renderEvent12);
      if (renderEvent12.aik()) {
         callbackInfo.cancel();
      }
   }
}
