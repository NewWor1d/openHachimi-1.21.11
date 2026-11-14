package pw.hachimi.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.module.misc.Tooltips;

@Mixin(DrawContext.class)
public class DrawContextMixin {
   @Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/render/state/GuiRenderState;II)V", at = @At("TAIL"))
   private void hookInit(net.minecraft.client.MinecraftClient client, net.minecraft.client.gui.render.state.GuiRenderState state, int mouseX, int mouseY, CallbackInfo callbackInfo) {
      pw.hachimi.client.render.CompatRender.setContext(DrawContext.class.cast(this));
   }

   @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
   public void onDrawItem(TextRenderer textRenderer, ItemStack stack, int n, int n2, String str, CallbackInfo callbackInfo) {
      Tooltips.aez().aeD(DrawContext.class.cast(this), stack, n, n2);
   }
}
