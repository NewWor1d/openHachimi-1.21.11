package pw.hachimi.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.render.CompatRender;

/**
 * 1.21.11 的 GUI 采用「先收集状态、再统一渲染」的两段式流程。
 * 客户端原有的即时绘制发生在收集阶段，会被 GuiRenderer 随后画的状态覆盖，
 * 所以打开界面时把这些绘制缓存下来，等 GuiRenderer 画完统一补上。
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
   @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
   private void hookFlushBatch(GpuBufferSlice slice, CallbackInfo callbackInfo) {
      CompatRender.flushGuiBatch();
   }

   @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("TAIL"))
   private void hookFlushDeferred(GpuBufferSlice slice, CallbackInfo callbackInfo) {
      CompatRender.flushDeferred();
      CompatRender.setGuiPhase(false);
   }
}
