package pw.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHud.ChatState;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.api.UnidentifiedInterface3;
import pw.hachimi.client.api.UnidentifiedInterface6;
import pw.hachimi.client.event.ClearEvent;
import pw.hachimi.client.event.network.AddMessageEvent;
import pw.hachimi.client.event.network.AddVisibleMessageEvent;
import pw.hachimi.client.handler.TextHandler;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements TextHandler {
   @Shadow
   @Final
   private List messages;
   @Shadow
   @Final
   private List visibleMessages;
   @Unique
   private int n;

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract void addMessage(Text var1, @Nullable MessageSignatureData var2, @Nullable MessageIndicator var3);

   // 1.21.11: ChatHud.render 改为 (DrawContext, TextRenderer, int,int,int, boolean,boolean)，
   // 逐行文本改由私有 render(Backend;IIZ) 里的 Backend.text(IFLOrderedText;)Z 输出。
   // 旧实现钩的是 DrawContext.drawTextWithShadow；现在在公有 render 里把 Backend 包一层。
   @Unique
   private DrawContext hachi$context;

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
   private void hachi$captureContext(
      DrawContext drawContext, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo callbackInfo
   ) {
      this.hachi$context = drawContext;
   }

   // ChatHud 的逐行文本在匿名 ChatHud$1.accept 里回调 Backend，不能直接按方法注入；
   // 改为在公有 render(DrawContext,...) 调用私有 render(Backend;IIZ)V 时把 Backend 包一层
   // （ChatHudBackendWrapper），由包装类统一接管 text/indicator。
   @WrapOperation(
      method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V")
   )
   private void hachi$wrapBackend(ChatHud chatHud, ChatHud.Backend backend, int windowHeight, int currentTick, boolean expanded, Operation operation) {
      ChatHud.Backend wrapped = this.hachi$context == null
         ? backend
         : new pw.hachimi.client.render.ChatHudBackendWrapper(backend, this.hachi$context, this.visibleMessages);
      operation.call(new Object[]{chatHud, wrapped, windowHeight, currentTick, expanded});
   }

   @Inject(method = "clear(Z)V", at = @At("HEAD"), cancellable = true)
   private void onClear(boolean flag, CallbackInfo callbackInfo) {
      if (flag) {
         ClearEvent clearEvent = new ClearEvent();
         EventBusImpl.INSTANCE.rC(clearEvent);
         if (clearEvent.aik()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "restoreChatState(Lnet/minecraft/client/gui/hud/ChatHud$ChatState;)V", at = @At("HEAD"), cancellable = true)
   private void onRestore(ChatState chatState, CallbackInfo callbackInfo) {
      ClearEvent clearEvent = new ClearEvent();
      EventBusImpl.INSTANCE.rC(clearEvent);
      if (clearEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
   private void addMessage$Head(Text text2, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo callbackInfo) {
      if (this.n != 0) {
         try {
            this.visibleMessages.removeIf(visible -> ((UnidentifiedInterface6)(Object)visible).Dh() == this.n);
            this.messages.removeIf(chatHudLine -> ((UnidentifiedInterface3)(Object)chatHudLine).et() == this.n);
         } catch (Exception exception) {
            exception.printStackTrace();
         }
      }
   }

   @WrapOperation(method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2))
   public int addVisibleMessage$size(List list, Operation operation) {
      AddVisibleMessageEvent addVisibleMessageEvent = new AddVisibleMessageEvent();
      EventBusImpl.INSTANCE.rC(addVisibleMessageEvent);
      boolean flag = addVisibleMessageEvent.aik();
      return flag ? -2147483647 : (Integer)operation.call(new Object[]{list});
   }

   @WrapOperation(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At(value = "NEW", target = "Lnet/minecraft/client/gui/hud/ChatHudLine;"))
   private ChatHudLine addMessage$new(int n2, Text text2, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, Operation operation) {
      AddMessageEvent addMessageEvent = new AddMessageEvent(text2);
      EventBusImpl.INSTANCE.rC(addMessageEvent);
      return (ChatHudLine)operation.call(new Object[]{n2, addMessageEvent.getText(), messageSignatureData, messageIndicator});
   }

   @WrapOperation(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
   public int addMessage$size(List list, Operation operation) {
      AddVisibleMessageEvent addVisibleMessageEvent = new AddVisibleMessageEvent();
      EventBusImpl.INSTANCE.rC(addVisibleMessageEvent);
      boolean flag = addVisibleMessageEvent.aik();
      return flag ? -2147483647 : (Integer)operation.call(new Object[]{list});
   }

   @WrapOperation(method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "NEW", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;"))
   private Visible addVisibleMessage$addChatLine(int n2, OrderedText orderedText, MessageIndicator messageIndicator, boolean flag, Operation operation) {
      Visible visible = (Visible)operation.call(new Object[]{n2, orderedText, messageIndicator, flag});
      ((UnidentifiedInterface6)(Object)visible).Di(this.n);
      return visible;
   }

   @Override
   public void Rn(Text text2, MessageIndicator messageIndicator, int n2) {
      this.n = n2;
      this.addMessage(text2, null, messageIndicator);
      this.n = 0;
   }
}
