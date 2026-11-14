package pw.hachimi.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.gui.TextDrawScreen;
import pw.hachimi.client.module.ColorHelper3;

/**
 * 1.21.11：全景背景方法从 TitleScreen 移到了 Screen#renderPanoramaBackground(DrawContext, float)。
 * 这里挂到 Screen 上，仅对 TitleScreen 生效，保持自定义主菜单背景。
 */
@Mixin(Screen.class)
public class ScreenPanoramaMixin {
   @Inject(method = "renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
   public void hookPanorama(DrawContext context, float delta, CallbackInfo callbackInfo) {
      if ((Object)this instanceof TitleScreen && ColorHelper3.auO().auQ()) {
         callbackInfo.cancel();
         TextDrawScreen.ez(context, ((Screen)(Object)this).width, ((Screen)(Object)this).height);
      }
   }
}
