package pw.hachimi.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderFireOverlayEvent;
import pw.hachimi.client.event.render.RenderFireOverlayEvent2;
import pw.hachimi.client.event.render.RenderUnderwaterOverlayEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
   @Inject(method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V", at = @At("HEAD"), cancellable = true)
   private static void hookRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, Sprite sprite, CallbackInfo callbackInfo) {
      RenderFireOverlayEvent2 renderFireOverlayEvent2 = new RenderFireOverlayEvent2(null);
      EventBusImpl.INSTANCE.rC(renderFireOverlayEvent2);
      if (renderFireOverlayEvent2.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderUnderwaterOverlay(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
   private static void hookRenderUnderwaterOverlay(MinecraftClient client2, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
      RenderUnderwaterOverlayEvent renderUnderwaterOverlayEvent = new RenderUnderwaterOverlayEvent(null);
      EventBusImpl.INSTANCE.rC(renderUnderwaterOverlayEvent);
      if (renderUnderwaterOverlayEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderInWallOverlay(Lnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
   private static void hookRenderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
      RenderFireOverlayEvent renderFireOverlayEvent = new RenderFireOverlayEvent(null);
      EventBusImpl.INSTANCE.rC(renderFireOverlayEvent);
      if (renderFireOverlayEvent.aik()) {
         callbackInfo.cancel();
      }
   }
}
