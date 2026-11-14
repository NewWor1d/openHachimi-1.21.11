package pw.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pw.hachimi.client.event.IsEmptyEvent;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11：物品框渲染改用 ItemRenderState，原来对 ItemStack.isEmpty() 的包装
 * 改为包装 ItemRenderState.isEmpty()。
 */
@Mixin(ItemFrameEntityRenderer.class)
public class ItemFrameEntityRendererMixin {
   @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/ItemFrameEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState;isEmpty()Z")
   )
   private boolean redirectIsEmpty(ItemRenderState state, Operation<Boolean> operation) {
      IsEmptyEvent isEmptyEvent = new IsEmptyEvent();
      EventBusImpl.INSTANCE.rC(isEmptyEvent);
      return isEmptyEvent.aik() ? true : operation.call(state);
   }
}
