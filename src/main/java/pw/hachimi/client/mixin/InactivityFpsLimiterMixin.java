package pw.hachimi.client.mixin;

import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.event.GetFramelimitEvent;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：MinecraftClient.getFramerateLimit() 被删除，帧率上限改由 InactivityFpsLimiter.update()
 * 计算（内部读 GameOptions.getMaxFps）。这里在返回值上挂 GetFramelimitEvent，恢复 Framelimit / AdaptiveFPS 模块。
 */
@Mixin(InactivityFpsLimiter.class)
public class InactivityFpsLimiterMixin {
   @Inject(method = "update()I", at = @At("RETURN"), cancellable = true)
   private void hookUpdate(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
      GetFramelimitEvent getFramelimitEvent = new GetFramelimitEvent();
      EventBusImpl.INSTANCE.rC(getFramelimitEvent);
      if (getFramelimitEvent.aik()) {
         callbackInfoReturnable.setReturnValue(getFramelimitEvent.arH());
      }
   }
}
