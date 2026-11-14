package pw.hachimi.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.render.CompatRender;

/**
 * 1.21.11：HUD 和 GUI 一样是「先收集状态、GuiRenderer 统一绘制」。
 * 在 InGameHud.render 入口打开 GUI 阶段标记，CompatRender 会把这一阶段的即时绘制
 * 转成 GuiRenderState 元素，避免被 GuiRenderer 覆盖（HUD 模块因此才能显示）。
 */
@Mixin(InGameHud.class)
public class HudRenderPhaseMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
   private void hookRenderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
      CompatRender.setGuiPhase(true);
   }
}
