package pw.hachimi.client.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderLabelIfPresentEvent;
import pw.hachimi.client.render.RenderStateRegistry;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：renderLabelIfPresent 的参数从 (Entity, Text, MatrixStack, VertexConsumerProvider, int, float)
 * 变成 (EntityRenderState, MatrixStack, OrderedRenderCommandQueue, CameraRenderState)。
 *
 * <p>这里在 updateRenderState 时记录 state→Entity，渲染钩子里反查；拿不到实体时按原逻辑放行。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
   @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
      at = @At("HEAD")
   )
   private void captureRenderState(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo callbackInfo) {
      RenderStateRegistry.putEntity(state, entity);
   }

   @Inject(
      method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   public void hookRenderLabelIfPresent(
      EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo callbackInfo
   ) {
      Entity entity = RenderStateRegistry.getEntity(state);
      if (entity == null) {
         return;
      }

      RenderLabelIfPresentEvent renderLabelIfPresentEvent = new RenderLabelIfPresentEvent(entity);
      EventBusImpl.INSTANCE.rC(renderLabelIfPresentEvent);
      if (renderLabelIfPresentEvent.aik()) {
         callbackInfo.cancel();
      }
   }
}
