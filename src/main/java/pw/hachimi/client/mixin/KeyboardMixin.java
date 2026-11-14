package pw.hachimi.client.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.OnKeyEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(Keyboard.class)
public class KeyboardMixin {
   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", at = @At("HEAD"), cancellable = true)
   private void hookOnKey(long l, int action, net.minecraft.client.input.KeyInput keyInput, CallbackInfo callbackInfo) {
      if (this.client.getWindow().getHandle() == l) {
         if (System.getProperty("hachimi.testui") != null) {
            System.out.println("[HachimiKey] key=" + keyInput.key() + " action=" + action);
         }

         OnKeyEvent onKeyEvent = new OnKeyEvent(keyInput.key(), action);
         EventBusImpl.INSTANCE.rC(onKeyEvent);
         if (onKeyEvent.aik()) {
            callbackInfo.cancel();
         }
      }
   }
}
