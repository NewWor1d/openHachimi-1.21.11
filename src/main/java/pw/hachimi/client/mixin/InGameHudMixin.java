package pw.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.Mc;
import pw.hachimi.client.event.player.SelectedSlotXEvent;
import pw.hachimi.client.event.render.RenderCrosshairEvent;
import pw.hachimi.client.event.render.RenderEvent;
import pw.hachimi.client.event.render.RenderHeldItemTooltipEvent;
import pw.hachimi.client.event.render.RenderHotbarEvent;
import pw.hachimi.client.event.render.RenderOverlayEvent;
import pw.hachimi.client.event.render.RenderOverlayEvent2;
import pw.hachimi.client.event.render.RenderPortalOverlayEvent;
import pw.hachimi.client.event.render.RenderScoreboardSidebarEvent;
import pw.hachimi.client.event.render.RenderSpyglassOverlayEvent;
import pw.hachimi.client.event.render.RenderStatusEffectOverlayEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(InGameHud.class)
public class InGameHudMixin {

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void hookRender(DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      RenderEvent renderEvent = new RenderEvent(context, renderTickCounter.getTickProgress(true));
      EventBusImpl.INSTANCE.rC(renderEvent);
   }

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderScoreboardSidebar(DrawContext context, ScoreboardObjective scoreboardObjective, CallbackInfo callbackInfo) {
      RenderScoreboardSidebarEvent renderScoreboardSidebarEvent = new RenderScoreboardSidebarEvent();
      EventBusImpl.INSTANCE.rC(renderScoreboardSidebarEvent);
      if (renderScoreboardSidebarEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderScoreboardSidebar(DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      RenderScoreboardSidebarEvent renderScoreboardSidebarEvent = new RenderScoreboardSidebarEvent();
      EventBusImpl.INSTANCE.rC(renderScoreboardSidebarEvent);
      if (renderScoreboardSidebarEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void hookRenderStatusEffectOverlay(DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      RenderStatusEffectOverlayEvent renderStatusEffectOverlayEvent = new RenderStatusEffectOverlayEvent(context);
      EventBusImpl.INSTANCE.rC(renderStatusEffectOverlayEvent);
      if (renderStatusEffectOverlayEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
   private void hookRenderPortalOverlay(DrawContext context, float f, CallbackInfo callbackInfo) {
      RenderPortalOverlayEvent renderPortalOverlayEvent = new RenderPortalOverlayEvent(context);
      EventBusImpl.INSTANCE.rC(renderPortalOverlayEvent);
      if (renderPortalOverlayEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderSpyglassOverlay(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
   private void hookRenderSpyglassOverlay(DrawContext context, float f, CallbackInfo callbackInfo) {
      RenderSpyglassOverlayEvent renderSpyglassOverlayEvent = new RenderSpyglassOverlayEvent(context);
      EventBusImpl.INSTANCE.rC(renderSpyglassOverlayEvent);
      if (renderSpyglassOverlayEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V", at = @At("HEAD"), cancellable = true)
   private void hookRenderOverlay(DrawContext context, Identifier id, float f, CallbackInfo callbackInfo) {
      if (id.getPath().equals("textures/misc/pumpkinblur.png")) {
         RenderOverlayEvent2 renderOverlayEvent2 = new RenderOverlayEvent2(context);
         EventBusImpl.INSTANCE.rC(renderOverlayEvent2);
         if (renderOverlayEvent2.aik()) {
            callbackInfo.cancel();
         }
      } else if (id.getPath().equals("textures/misc/powder_snow_outline.png")) {
         RenderOverlayEvent renderOverlayEvent = new RenderOverlayEvent(context);
         EventBusImpl.INSTANCE.rC(renderOverlayEvent);
         if (renderOverlayEvent.aik()) {
            callbackInfo.cancel();
         }
      }
   }

   @WrapOperation(
      method = "renderHeldItemTooltip(Lnet/minecraft/client/gui/DrawContext;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithBackground(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIII)V")
   )
   private void hookRenderHeldItemTooltip(DrawContext context, TextRenderer textRenderer2, Text text2, int n, int n2, int n3, int n4, Operation operation) {
      RenderHeldItemTooltipEvent renderHeldItemTooltipEvent = new RenderHeldItemTooltipEvent(context, text2);
      EventBusImpl.INSTANCE.rC(renderHeldItemTooltipEvent);
      if (renderHeldItemTooltipEvent.aik()) {
         if (renderHeldItemTooltipEvent.gB()) {
            context.drawText(Mc.client.textRenderer, renderHeldItemTooltipEvent.getText(), renderHeldItemTooltipEvent.gC(), renderHeldItemTooltipEvent.gE(), n4, true);
         }
      } else {
         operation.call(new Object[]{context, textRenderer2, text2, n, n2, n3, n4});
      }
   }

   @Inject(method = "renderMainHud(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void hookRenderHotbar(DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      RenderHotbarEvent renderHotbarEvent = new RenderHotbarEvent(context);
      EventBusImpl.INSTANCE.rC(renderHotbarEvent);
   }

   @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void hookRenderCrosshair(DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      RenderCrosshairEvent renderCrosshairEvent = new RenderCrosshairEvent(context);
      EventBusImpl.INSTANCE.rC(renderCrosshairEvent);
      if (renderCrosshairEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @WrapOperation(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 1)
   )
   private void selectedSlotX(
      DrawContext context, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, Identifier id, int n, int n2, int n3, int n4, Operation operation
   ) {
      SelectedSlotXEvent selectedSlotXEvent = new SelectedSlotXEvent(context, id, n, n2, n3, n4);
      EventBusImpl.INSTANCE.rC(selectedSlotXEvent);
      if (!selectedSlotXEvent.aik()) {
         operation.call(new Object[]{context, pipeline, id, n, n2, n3, n4});
      }
   }
}
