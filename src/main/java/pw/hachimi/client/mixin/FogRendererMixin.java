package pw.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogModifier;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pw.hachimi.client.event.render.ApplyFogEvent;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：BackgroundRenderer 被删除，雾改成 FogRenderer + FogModifier 列表。
 * 原来的 ApplyFogEvent（地形雾起止距离）挂在每次 FogModifier.applyStartEndModifier 之后，
 * 事件取消时把自定义值写回 FogData。
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
   @WrapOperation(
      method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/fog/FogModifier;applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V"
      )
   )
   private void hookApplyStartEndModifier(
      FogModifier modifier, FogData data, Camera camera, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, Operation<Void> operation
   ) {
      operation.call(modifier, data, camera, world, viewDistance, tickCounter);
      ApplyFogEvent applyFogEvent = new ApplyFogEvent(viewDistance);
      EventBusImpl.INSTANCE.rC(applyFogEvent);
      if (applyFogEvent.aik()) {
         data.environmentalStart = applyFogEvent.VS();
         data.environmentalEnd = applyFogEvent.VU();
      }
   }
}
