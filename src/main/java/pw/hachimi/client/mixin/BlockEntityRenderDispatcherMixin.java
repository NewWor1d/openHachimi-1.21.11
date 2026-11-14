package pw.hachimi.client.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.hachimi.client.event.render.RenderEvent9;
import pw.hachimi.client.render.RenderStateRegistry;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：方块实体渲染改为
 * {@code render(BlockEntityRenderState, MatrixStack, OrderedRenderCommandQueue, CameraRenderState)}，
 * render 阶段拿不到 BlockEntity。这里在 {@code getRenderState} 返回时用
 * {@link RenderStateRegistry} 记录 state → BlockEntity，渲染钩子里反查。
 */
@Mixin(BlockEntityRenderManager.class)
public class BlockEntityRenderDispatcherMixin {
   @Inject(
      method = "getRenderState(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;",
      at = @At("RETURN")
   )
   private void captureRenderState(
      BlockEntity blockEntity, float tickProgress, CrumblingOverlayCommand crumblingOverlay, CallbackInfoReturnable<BlockEntityRenderState> callbackInfoReturnable
   ) {
      RenderStateRegistry.putBlockEntity(callbackInfoReturnable.getReturnValue(), blockEntity);
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD")
   )
   private void hookRender(
      BlockEntityRenderState blockEntityRenderState,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      CameraRenderState cameraState,
      CallbackInfo callbackInfo
   ) {
      BlockEntity blockEntity = RenderStateRegistry.getBlockEntity(blockEntityRenderState);
      if (blockEntity != null) {
         EventBusImpl.INSTANCE.rC(new RenderEvent9(blockEntity));
      }
   }
}
