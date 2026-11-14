package pw.hachimi.client.mixin;

import net.minecraft.client.render.entity.WitherSkullEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderEvent6;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(WitherSkullEntityRenderer.class)
public class WitherSkullEntityRendererMixin {
   @Inject(method = "render(Lnet/minecraft/client/render/entity/state/WitherSkullEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
   private void hookRender(net.minecraft.client.render.entity.state.WitherSkullEntityRenderState state, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo callbackInfo) {
      RenderEvent6 renderEvent6 = new RenderEvent6();
      EventBusImpl.INSTANCE.rC(renderEvent6);
      if (renderEvent6.aik()) {
         callbackInfo.cancel();
      }
   }
}
