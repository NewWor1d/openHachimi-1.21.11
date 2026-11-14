package pw.hachimi.client.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.event.render.GetCloudsColorEvent;
import pw.hachimi.client.event.render.GetFogColorOverrideEvent;
import pw.hachimi.client.event.render.GetSkyColorEvent;
import pw.hachimi.client.event.render.GetWaterColorEvent;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11 起雾/天空/云的颜色统一走 EnvironmentAttributes，Biome#getFogColor、
 * ClientWorld#getSkyColor/getCloudsColor 都被删除。所有渲染路径最终都会调用
 * {@link EnvironmentAttributeInterpolator#get(EnvironmentAttribute, float)}，
 * 这里按属性种类触发原来的颜色覆盖事件，保持 Skybox 等模块的行为。
 */
@Mixin(EnvironmentAttributeInterpolator.class)
public class EnvironmentAttributeInterpolatorMixin {
   @SuppressWarnings("unchecked")
   @Inject(method = "get(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
   private <Value> void hookGet(EnvironmentAttribute<Value> attribute, float tickProgress, CallbackInfoReturnable<Value> callbackInfoReturnable) {
      if (attribute == EnvironmentAttributes.FOG_COLOR_VISUAL) {
         GetFogColorOverrideEvent event = new GetFogColorOverrideEvent(tickProgress);
         EventBusImpl.INSTANCE.rC(event);
         if (event.aik()) {
            callbackInfoReturnable.setReturnValue((Value)Integer.valueOf(event.arN()));
         }
      } else if (attribute == EnvironmentAttributes.WATER_FOG_COLOR_VISUAL) {
         GetWaterColorEvent event = new GetWaterColorEvent();
         EventBusImpl.INSTANCE.rC(event);
         if (event.aik()) {
            callbackInfoReturnable.setReturnValue((Value)Integer.valueOf(event.MQ()));
         }
      } else if (attribute == EnvironmentAttributes.SKY_COLOR_VISUAL) {
         GetSkyColorEvent event = new GetSkyColorEvent();
         EventBusImpl.INSTANCE.rC(event);
         if (event.aik()) {
            callbackInfoReturnable.setReturnValue((Value)Integer.valueOf(event.arN()));
         }
      } else if (attribute == EnvironmentAttributes.CLOUD_COLOR_VISUAL) {
         GetCloudsColorEvent event = new GetCloudsColorEvent();
         EventBusImpl.INSTANCE.rC(event);
         if (event.aik()) {
            callbackInfoReturnable.setReturnValue((Value)Integer.valueOf(event.arN()));
         }
      }
   }
}
