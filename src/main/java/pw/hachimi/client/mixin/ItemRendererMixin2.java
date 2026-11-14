package pw.hachimi.client.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.model.render.FastHideMode;
import pw.hachimi.client.module.render.NoRender;

/**
 * 1.21.11 物品渲染改成了 ItemModelManager + ItemRenderState 管线，旧的
 * ItemRenderer.renderItem(..., BakedModel) 与 ModelTransformationMode 都不存在了。
 *
 * <p>这里保留 NoRender 的 FAST 隐藏掉落物功能：在 ItemRenderState.render 入口按
 * 显示上下文（GROUND）取消渲染。原实现额外的 "!bakedModel.hasDepth()" 判断
 * （只隐藏平面物品）在新模型系统里没有等价物，这里按显示上下文统一处理。
 */
@Mixin(ItemRenderState.class)
public abstract class ItemRendererMixin2 {
   @Shadow
   ItemDisplayContext displayContext;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void hookRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, int seed, CallbackInfo callbackInfo) {
      NoRender noRender = NoRender.ff();
      if (noRender.awg()
         && noRender.items.atx() == FastHideMode.FAST
         && !(Boolean)noRender.sidesOfItems.atx()
         && this.displayContext == ItemDisplayContext.GROUND) {
         callbackInfo.cancel();
      }
   }
}
