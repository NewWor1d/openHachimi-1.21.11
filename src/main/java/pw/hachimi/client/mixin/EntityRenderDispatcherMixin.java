package pw.hachimi.client.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.event.render.RenderEvent5;
import pw.hachimi.client.render.RenderStateRegistry;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：EntityRenderManager.render 改为
 * {@code render(EntityRenderState, CameraRenderState, double,double,double, MatrixStack, OrderedRenderCommandQueue)}，
 * render 阶段拿不到 Entity；在 {@code getAndUpdateRenderState} 返回时记录 state → Entity，
 * 渲染结束时用反查到的实体触发原来的 RenderEvent5。
 *
 * <p>新签名没有 yaw/pitch 参数，事件里用 {@code entity.getYaw()/getPitch()} 填充；也没有
 * VertexConsumerProvider（1.21.11 改为命令队列），事件中该字段为 null。
 */
@Mixin(EntityRenderManager.class)
public class EntityRenderDispatcherMixin {
   @Inject(
      method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
      at = @At("RETURN")
   )
   private void captureRenderState(Entity entity, float tickProgress, CallbackInfoReturnable<EntityRenderState> callbackInfoReturnable) {
      RenderStateRegistry.putEntity(callbackInfoReturnable.getReturnValue(), entity);
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
      at = @At("RETURN")
   )
   private void hookRender(
      EntityRenderState entityRenderState,
      CameraRenderState cameraState,
      double offsetX,
      double offsetY,
      double offsetZ,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      CallbackInfo callbackInfo
   ) {
      Entity entity = RenderStateRegistry.getEntity(entityRenderState);
      if (entity != null) {
         EventBusImpl.INSTANCE
            .rC(new RenderEvent5(entity, offsetX, offsetY, offsetZ, entity.getYaw(), entity.getPitch(), matrices, null));
      }
   }
}
