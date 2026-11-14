package pw.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.Mc;
import pw.hachimi.client.event.TickEvent4;
import pw.hachimi.client.model.render.NormalHeadOnlyMode;
import pw.hachimi.client.module.HitResultWorldHelper;
import pw.hachimi.client.utils.player.PlayerMathHelperUtils;
import pw.hachimi.client.utils.player.RotationUtils;
import pw.hachimi.client.utils.world.EntityBoxUtils;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：玩家滑翔/披风的姿势计算整体搬进了 updateGliding(PlayerLikeEntity, PlayerEntityRenderState, float)，
 * 原来注入在 setupTransforms 里的 isGliding()/getRotationVec() 调用点已经不存在。
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
   @Inject(
      method = "updateGliding(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void hookUpdateGliding(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo callbackInfo) {
      if (Mc.client.player == player) {
         TickEvent4 tickEvent4 = new TickEvent4();
         EventBusImpl.INSTANCE.rC(tickEvent4);
         if (tickEvent4.aik()) {
            callbackInfo.cancel();
         }
      }
   }

   @WrapOperation(
      method = "updateGliding(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/PlayerLikeEntity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;")
   )
   private Vec3d hookSetupTransforms(PlayerLikeEntity player, float f, Operation<Vec3d> operation) {
      return Mc.client.player == player && HitResultWorldHelper.Sq().SN() != NormalHeadOnlyMode.None
         ? RotationUtils.nS(
            EntityBoxUtils.BB(PlayerMathHelperUtils.wP(), PlayerMathHelperUtils.wM(), f),
            EntityBoxUtils.BB(PlayerMathHelperUtils.wQ(), PlayerMathHelperUtils.wN(), f)
         )
         : operation.call(player, f);
   }
}
