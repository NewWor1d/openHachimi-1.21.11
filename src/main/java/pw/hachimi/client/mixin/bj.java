package pw.hachimi.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.module.client.Font;

/**
 * 1.21.11 的 {@code TextRenderer$Drawer} 只有一个 {@code color} 字段，主文字和阴影共用它。
 * 旧实现把 {@code Font.shadowBrightness}（默认 0.25）直接乘到构造参数上，
 * 等于把所有带 shadow 的原版文字（聊天、HUD、记分板）整体压到 25% 亮度。
 *
 * <p>这里改成只调整阴影色 {@code getShadowColor} 的返回值：主文字保持原亮度，
 * 阴影亮度仍由 ShadowBrightness 控制（默认 0.25 与 vanilla 一致，直接跳过）。
 */
@Mixin(targets = "net/minecraft/client/font/TextRenderer$Drawer")
public class bj {
   @Inject(method = "getShadowColor", at = @At("RETURN"), cancellable = true, require = 0)
   private void hachimi$shadowBrightness(Style style, int textColor, CallbackInfoReturnable<Integer> callbackInfoReturnable) {
      if (Font.kT() == null || style.getShadowColor() != null) {
         return;
      }

      float brightness = Font.kT().la();
      if (Math.abs(brightness - 0.25F) < 0.001F) {
         return;
      }

      int shadow = callbackInfoReturnable.getReturnValue();
      float scale = brightness / 0.25F;
      int alpha = shadow & 0xFF000000;
      int red = Math.min(255, (int)((shadow >> 16 & 0xFF) * scale));
      int green = Math.min(255, (int)((shadow >> 8 & 0xFF) * scale));
      int blue = Math.min(255, (int)((shadow & 0xFF) * scale));
      callbackInfoReturnable.setReturnValue(alpha | red << 16 | green << 8 | blue);
   }
}
