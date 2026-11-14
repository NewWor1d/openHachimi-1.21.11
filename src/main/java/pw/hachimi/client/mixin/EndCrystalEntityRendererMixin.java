package pw.hachimi.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.render.RenderEvent3;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11 的末影水晶渲染改为 updateRenderState + render(RenderState, MatrixStack, Queue, CameraState)。
 * 旧的 EndCrystalEntityRendererMixin 已移入 attic，导致 RenderEvent3 从来没人发，
 * Chams / CrystalModel / Shaders 的水晶处理全部失效。这里按新签名重建：
 * 在 updateRenderState 里记住实体，在 render 头部发事件并支持取消。
 */
@Mixin(EndCrystalEntityRenderer.class)
public class EndCrystalEntityRendererMixin {
   @Shadow
   @Final
   private EndCrystalEntityModel model;

   @Unique
   private EndCrystalEntity hachimi$crystal;

   @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V",
      at = @At("HEAD")
   )
   private void hachimi$capture(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickProgress, CallbackInfo callbackInfo) {
      this.hachimi$crystal = entity;
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void hachimi$render(
      EndCrystalEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo callbackInfo
   ) {
      if (this.hachimi$crystal == null) {
         return;
      }

      RenderEvent3 event = new RenderEvent3(
         this.hachimi$crystal,
         0.0F,
         state.age,
         matrices,
         state.light,
         this.model.cube,
         this.model.base,
         MinecraftClient.getInstance().getEntityRenderDispatcher()
      );
      EventBusImpl.INSTANCE.rC(event);
      if (event.aik()) {
         callbackInfo.cancel();
      }
   }
}
