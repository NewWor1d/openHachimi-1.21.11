package pw.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.Mc;
import pw.hachimi.client.event.render.RenderPreEvent;
import pw.hachimi.client.event.render.Render_isInvisibleToEvent;
import pw.hachimi.client.model.render.NormalHeadOnlyMode;
import pw.hachimi.client.module.HitResultWorldHelper;
import pw.hachimi.client.utils.player.PlayerMathHelperUtils;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11 把实体渲染拆成 updateRenderState（算姿势）+ render(RenderState, MatrixStack, Queue, CameraState)。
 * 原来注入在 render(...) 里的 lerp / isInvisibleTo 现在都在 updateRenderState / clampBodyYaw 里。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
   @Shadow
   @Final
   public List features;
   @Shadow
   protected EntityModel model;
   @Unique
   LivingEntity u;

   @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At("HEAD")
   )
   private void captureEntity(LivingEntity living, LivingEntityRenderState state, float tickProgress, CallbackInfo callbackInfo) {
      this.u = living;
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   public void onRenderPre(
      LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo callbackInfo
   ) {
      if (this.u == null) {
         return;
      }

      RenderPreEvent renderPreEvent = new RenderPreEvent(
         (LivingEntityRenderer)(Object)this,
         this.u,
         state.bodyYaw,
         state.relativeHeadYaw,
         matrices,
         null,
         0,
         this.model,
         this.getRenderLayer(state, false, false, false),
         this.features
      );
      EventBusImpl.INSTANCE.rC(renderPreEvent);
      if (renderPreEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @WrapOperation(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 0)
   )
   private float render$headYaw(float f, float f2, float f3, Operation<Float> operation) {
      return this.u == Mc.client.player && HitResultWorldHelper.Sq().SN() != NormalHeadOnlyMode.None
         ? PlayerMathHelperUtils.wK(f)
         : operation.call(f, f2, f3);
   }

   @WrapOperation(
      method = "clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 0)
   )
   private static float render$bodyYawRiding(float f, float f2, float f3, Operation<Float> operation, @Local(argsOnly = true) LivingEntity entity) {
      return overrideBodyYaw(entity, f, operation, f, f2, f3);
   }

   @WrapOperation(
      method = "clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 1)
   )
   private static float render$bodyYaw(float f, float f2, float f3, Operation<Float> operation, @Local(argsOnly = true) LivingEntity entity) {
      return overrideBodyYaw(entity, f, operation, f, f2, f3);
   }

   @Unique
   private static float overrideBodyYaw(LivingEntity entity, float f, Operation<Float> operation, float f2, float f3, float f4) {
      if (entity == Mc.client.player) {
         if (HitResultWorldHelper.Sq().SN() == NormalHeadOnlyMode.BodyFixed) {
            return PlayerMathHelperUtils.wK(f);
         }

         if (HitResultWorldHelper.Sq().SN() == NormalHeadOnlyMode.Normal) {
            return PlayerMathHelperUtils.wJ(f);
         }
      }

      return operation.call(f2, f3, f4);
   }

   @WrapOperation(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F")
   )
   private float render$pitch(LivingEntity living, float tickProgress, Operation<Float> operation) {
      return this.u == Mc.client.player && HitResultWorldHelper.Sq().SN() != NormalHeadOnlyMode.None
         ? PlayerMathHelperUtils.wL(tickProgress)
         : operation.call(living, tickProgress);
   }

   @Shadow
   protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

   @WrapOperation(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z")
   )
   private boolean redirectRender$isInvisibleTo(LivingEntity living, PlayerEntity player2, Operation<Boolean> operation) {
      Render_isInvisibleToEvent renderisInvisibleToEvent = new Render_isInvisibleToEvent(player2);
      EventBusImpl.INSTANCE.rC(renderisInvisibleToEvent);
      return renderisInvisibleToEvent.aik() ? false : operation.call(living, player2);
   }
}
