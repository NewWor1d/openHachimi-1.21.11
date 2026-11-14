package pw.hachimi.client.render;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import pw.hachimi.client.event.render.DrawTextWithShadowHookEvent;
import pw.hachimi.client.event.render.DrawTextWithShadowHookEvent2;
import pw.hachimi.client.event.render.RenderEvent13;
import pw.hachimi.client.manager.RenderUtils;
import pw.hachimi.client.utils.chat.StyleFormattingUtils;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11 ChatHud 的 Backend 桥接：逐行文本与消息指示器都经由 Backend 回调，
 * 这里包一层以恢复 Font 模块的自定义字体（RenderUtils.acE）和指示器隐藏事件。
 *
 * <p>由 {@code ChatHudMixin} 在公有 render(DrawContext, TextRenderer, ...) 调用私有
 * render(Backend, int, int, boolean) 时注入。
 */
public final class ChatHudBackendWrapper implements ChatHud.Backend {
   private final ChatHud.Backend delegate;
   private final DrawContext context;
   private final List visibleMessages;
   private boolean hideIndicator;

   public ChatHudBackendWrapper(ChatHud.Backend delegate, DrawContext context, List visibleMessages) {
      this.delegate = delegate;
      this.context = context;
      this.visibleMessages = visibleMessages;
   }

   @Override
   public void updatePose(Consumer<Matrix3x2f> transformer) {
      this.delegate.updatePose(transformer);
   }

   @Override
   public void fill(int x1, int y1, int x2, int y2, int color) {
      this.delegate.fill(x1, y1, x2, y2, color);
   }

   @Override
   public boolean text(int y, float opacity, OrderedText text) {
      DrawTextWithShadowHookEvent2 event2 = new DrawTextWithShadowHookEvent2(this.findVisible(text));
      EventBusImpl.INSTANCE.rC(event2);
      DrawTextWithShadowHookEvent event1 = new DrawTextWithShadowHookEvent();
      EventBusImpl.INSTANCE.rC(event1);
      int alpha = MathHelper.clamp((int)(opacity * 255.0F), 0, 255);
      int color = alpha << 24 | 16777215;
      if (event2.aik()) {
         if (event2.Ho()) {
            int x = (int)event2.Hk();
            RenderUtils.acE(this.context, StyleFormattingUtils.Es(text), x, y, color);
         } else {
            float fade = Math.clamp((float)event2.Hk(), 0.0F, 1.0F);
            int fadeAlpha = Math.max(10, (int)(alpha * fade));
            int fadeColor = fade == 1.0F ? color : fadeAlpha << 24 | color & 16777215;
            RenderUtils.acE(this.context, StyleFormattingUtils.Es(text), 0.0F, y, fadeColor);
         }

         return false;
      }

      if (event1.aik()) {
         RenderUtils.acE(this.context, StyleFormattingUtils.Es(text), 0.0F, y, color);
         return false;
      }

      return this.delegate.text(y, opacity, text);
   }

   @Override
   public void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator) {
      RenderEvent13 event = new RenderEvent13();
      EventBusImpl.INSTANCE.rC(event);
      this.hideIndicator = event.aik();
      if (!this.hideIndicator) {
         this.delegate.indicator(x1, y1, x2, y2, opacity, indicator);
      }
   }

   @Override
   public void indicatorIcon(int left, int bottom, boolean forceDraw, MessageIndicator indicator, MessageIndicator.Icon icon) {
      if (!this.hideIndicator) {
         this.delegate.indicatorIcon(left, bottom, forceDraw, indicator, icon);
      }
   }

   private Visible findVisible(OrderedText text) {
      if (this.visibleMessages == null) {
         return null;
      }

      for (Object object : this.visibleMessages) {
         Visible visible = (Visible)object;
         if (visible.content() == text) {
            return visible;
         }
      }

      return null;
   }
}
