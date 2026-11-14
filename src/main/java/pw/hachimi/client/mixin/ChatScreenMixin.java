package pw.hachimi.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.FillEvent;
import pw.hachimi.client.event.network.SendMessageEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends ScreenMixin {
   @Shadow
   protected TextFieldWidget chatField;

   @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
   private void hookSendMessage(String str, boolean flag, CallbackInfo callbackInfo) {
      SendMessageEvent sendMessageEvent = new SendMessageEvent(str);
      EventBusImpl.INSTANCE.rC(sendMessageEvent);
      if (sendMessageEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
   private void hookFill(DrawContext context, int n, int n2, int n3, int n4, int n5) {
      FillEvent fillEvent = new FillEvent();
      EventBusImpl.INSTANCE.rC(fillEvent);
      float f = fillEvent.aik() ? 12.0F * fillEvent.x() : 12.0F;
      context.fill(
         2,
         (int)(this.height - 2.0F),
         this.width - 2,
         (int)(this.height - 2.0F - f),
         this.client.options.getTextBackgroundColor(Integer.MIN_VALUE)
      );
   }

   // 1.21.11: ChatScreen.init() 通过 addDrawableChild(chatField) 把输入框注册进 drawables，
   // Screen.render() 只渲染 drawables 里的控件。这里原本被覆写成 return null，
   // 导致输入框永远不参与渲染 —— 背景 fill 还在、文字全没了。让原版逻辑正常注册。
}
