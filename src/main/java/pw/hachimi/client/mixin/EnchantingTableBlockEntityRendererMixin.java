package pw.hachimi.client.mixin;

import net.minecraft.client.render.block.entity.EnchantingTableBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.EnchantingTableBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderEvent10;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：附魔台渲染改为 render(EnchantingTableBlockEntityRenderState, MatrixStack, OrderedRenderCommandQueue, CameraRenderState)。
 * 原实现是在 BookModel.renderBook 调用前取消并手动 pop 矩阵；改成 HEAD 取消后不需要再 pop。
 */
@Mixin(EnchantingTableBlockEntityRenderer.class)
public class EnchantingTableBlockEntityRendererMixin {
   @Inject(
      method = "render(Lnet/minecraft/client/render/block/entity/state/EnchantingTableBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void hookRender(
      EnchantingTableBlockEntityRenderState state,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      CameraRenderState cameraState,
      CallbackInfo callbackInfo
   ) {
      RenderEvent10 renderEvent10 = new RenderEvent10();
      EventBusImpl.INSTANCE.rC(renderEvent10);
      if (renderEvent10.aik()) {
         callbackInfo.cancel();
      }
   }
}
