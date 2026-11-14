package pw.hachimi.client.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.EventPhase;
import pw.hachimi.client.event.Tick_PreEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
   @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
   private void hookTick$Pre(CallbackInfo callbackInfo) {
      Tick_PreEvent tickPreEvent = new Tick_PreEvent((Input)(Object)this);
      tickPreEvent.aqo(EventPhase.PRE);
      EventBusImpl.INSTANCE.rC(tickPreEvent);
      if (tickPreEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   // 1.21.11: KeyboardInput 不再逐字段赋值，POST 钩子改为 TAIL
   @Inject(method = "tick()V", at = @At("TAIL"), cancellable = true)
   private void hookTick$Post(CallbackInfo callbackInfo) {
      Tick_PreEvent tickPreEvent = new Tick_PreEvent((Input)(Object)this);
      tickPreEvent.aqo(EventPhase.POST);
      EventBusImpl.INSTANCE.rC(tickPreEvent);
      if (tickPreEvent.aik()) {
         callbackInfo.cancel();
      }
   }
}
