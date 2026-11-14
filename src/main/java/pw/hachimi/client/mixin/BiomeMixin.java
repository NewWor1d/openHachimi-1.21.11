package pw.hachimi.client.mixin;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.event.render.GetDefaultColorEvent;
import pw.hachimi.client.event.render.GetGrassColorAtEvent;
import pw.hachimi.client.event.render.GetWaterColorEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(Biome.class)
public class BiomeMixin {
   @Shadow
   @Final
   private BiomeEffects effects;

   // 1.21.11: Biome#getFogColor / getWaterFogColor 已删除，雾颜色统一由
   // EnvironmentAttributeInterpolatorMixin 通过 EnvironmentAttributes.*_COLOR_VISUAL 触发事件。

   @Inject(method = "getWaterColor()I", at = @At("HEAD"), cancellable = true)
   private void hookGetWaterColor(CallbackInfoReturnable callbackInfoReturnable) {
      GetWaterColorEvent getWaterColorEvent = new GetWaterColorEvent();
      EventBusImpl.INSTANCE.rC(getWaterColorEvent);
      if (getWaterColorEvent.aik()) {
         callbackInfoReturnable.cancel();
         callbackInfoReturnable.setReturnValue(getWaterColorEvent.MQ());
      }
   }

   @Inject(method = "getGrassColorAt(DD)I", at = @At("HEAD"), cancellable = true)
   private void hookGetGrassColorAt(double d, double d2, CallbackInfoReturnable callbackInfoReturnable) {
      GetGrassColorAtEvent getGrassColorAtEvent = new GetGrassColorAtEvent();
      EventBusImpl.INSTANCE.rC(getGrassColorAtEvent);
      if (getGrassColorAtEvent.aik()) {
         callbackInfoReturnable.cancel();
         int n = this.effects.grassColorModifier().getModifiedGrassColor(d, d2, getGrassColorAtEvent.MQ());
         callbackInfoReturnable.setReturnValue(n);
      }
   }

   @Inject(method = "getDefaultGrassColor()I", at = @At("HEAD"), cancellable = true)
   private void hookGetDefaultGrassColor(CallbackInfoReturnable callbackInfoReturnable) {
      GetGrassColorAtEvent getGrassColorAtEvent = new GetGrassColorAtEvent();
      EventBusImpl.INSTANCE.rC(getGrassColorAtEvent);
      if (getGrassColorAtEvent.aik()) {
         callbackInfoReturnable.cancel();
         callbackInfoReturnable.setReturnValue(getGrassColorAtEvent.MQ());
      }
   }

   @Inject(method = "getFoliageColor()I", at = @At("HEAD"), cancellable = true)
   private void hookGetFoliageColor(CallbackInfoReturnable callbackInfoReturnable) {
      GetDefaultColorEvent getDefaultColorEvent = new GetDefaultColorEvent();
      EventBusImpl.INSTANCE.rC(getDefaultColorEvent);
      if (getDefaultColorEvent.aik()) {
         callbackInfoReturnable.cancel();
         callbackInfoReturnable.setReturnValue(getDefaultColorEvent.MQ());
      }
   }
}
