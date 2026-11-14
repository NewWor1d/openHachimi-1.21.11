package pw.hachimi.client.mixin;

import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.event.render.BeginRenderTickEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(Dynamic.class)
public class RenderTickCounterDynamicMixin {
   @Shadow
   private float dynamicDeltaTicks;
   @Shadow
   private float tickProgress;
   @Shadow
   private long lastTimeMillis;
   @Final
   @Shadow
   private float tickTime;

   @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
   private void hookBeginRenderTick(long l, CallbackInfoReturnable callbackInfoReturnable) {
      BeginRenderTickEvent beginRenderTickEvent = new BeginRenderTickEvent();
      EventBusImpl.INSTANCE.rC(beginRenderTickEvent);
      if (beginRenderTickEvent.aik()) {
         this.dynamicDeltaTicks = (float)(l - this.lastTimeMillis) / this.tickTime * beginRenderTickEvent.qS();
         this.lastTimeMillis = l;
         this.tickProgress = this.tickProgress + this.dynamicDeltaTicks;
         int n = (int)this.tickProgress;
         this.tickProgress -= n;
         callbackInfoReturnable.setReturnValue(n);
      }
   }
}
