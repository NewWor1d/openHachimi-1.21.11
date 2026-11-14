package pw.hachimi.client.manager;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Optional;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import pw.hachimi.client.Mc;
import pw.hachimi.client.render.CompatRender;
import pw.hachimi.client.render.FramebufferFramebuffer3;
import pw.hachimi.client.render.OffscreenCopy;
import pw.hachimi.satin.ManagedShaderEffect;
import pw.hachimi.satin.PostEffect;
import pw.hachimi.satin.ShaderEffectManager;

/**
 * 1.21.11 迁移：
 * - {@code RenderLayer.MultiPhase}/{@code RenderPhase} 被删除，自定义描边层改用
 *   {@link net.minecraft.client.render.RenderLayers#outlineNoCull(Identifier)}；
 * - {@code OutlineVertexConsumerProvider} 不再包裹别的 provider，改为自带缓冲 + draw()；
 * - 后处理（menu/blur/outline 三个 post effect）依赖新的 PostEffectPipeline 格式，
 *   迁移完成前 {@link #VY} 只负责离屏渲染部分，不套后处理。
 *
 * <p>详细状态见 docs/PORT_1.21.11.md。
 */
public class FramebufferBuilderHelper {
   private final OutlineVertexConsumerProvider outlineVertexConsumerProvider = new OutlineVertexConsumerProvider();
   private final ManagedShaderEffect managedShaderEffect;
   private final ManagedShaderEffect managedShaderEffect2;
   private final ManagedShaderEffect managedShaderEffect3;
   private final FramebufferFramebuffer3 framebufferFramebuffer3 = new FramebufferFramebuffer3();

   public FramebufferBuilderHelper() {
      this.managedShaderEffect2 = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/menu.json"));
      this.managedShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/outline.json"));
      this.managedShaderEffect3 = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/blur.json"));
   }

   public void VY(ManagedShaderEffect effect, Runnable drawEffect, Runnable drawScene) {
      Framebuffer framebuffer = Mc.client.getFramebuffer();
      RenderSystem.assertOnRenderThread();
      if (this.framebufferFramebuffer3.textureWidth != framebuffer.textureWidth
         || this.framebufferFramebuffer3.textureHeight != framebuffer.textureHeight) {
         this.framebufferFramebuffer3.resize(framebuffer.textureWidth, framebuffer.textureHeight);
      }

      // 1) 场景（实体/控件）画进离屏 FBO，作为后效应的 Diffuse 输入。
      OffscreenCopy.clear(this.framebufferFramebuffer3);
      CompatRender.setTarget(this.framebufferFramebuffer3);
      try {
         drawScene.run();
      } finally {
         CompatRender.clearTarget();
      }

      PostEffect postEffect = effect.getShaderEffect();
      if (postEffect == null) {
         OffscreenCopy.clear(this.framebufferFramebuffer3);
         return;
      }

      // 2) 设置 uniform 并执行 pass（输出到内部 target:swap）。
      postEffect.setScene(this.framebufferFramebuffer3);
      drawEffect.run();

      // 3) 把 swap 合成回主 framebuffer（等价于旧版 framebuffer2.draw(...)）。
      // 注意 Framebuffer#drawBlit(GpuTextureView) 的参数是「目标视图」，
      // 它会把 this 的 colorTexture 作为 InSampler 画进该目标（原版用法：
      // entityOutlineFramebuffer.drawBlit(main.getColorAttachmentView())）。
      Framebuffer output = postEffect.getFramebuffer("swap");
      if (output != null && output.getColorAttachmentView() != null) {
         output.drawBlit(framebuffer.getColorAttachmentView());
      }
   }

   public OutlineVertexConsumerProvider getOutlineVertexConsumerProvider() {
      return this.outlineVertexConsumerProvider;
   }

   public VertexConsumerProvider Wa(VertexConsumerProvider vertexConsumerProvider, Color color) {
      return renderLayer -> this.Wf(vertexConsumerProvider, color, renderLayer);
   }

   public ManagedShaderEffect getManagedShaderEffect() {
      return this.managedShaderEffect;
   }

   public ManagedShaderEffect getManagedShaderEffect2() {
      return this.managedShaderEffect2;
   }

   public ManagedShaderEffect getManagedShaderEffect3() {
      return this.managedShaderEffect3;
   }

   VertexConsumer Wf(VertexConsumerProvider vertexConsumerProvider, Color color, RenderLayer renderLayer) {
      Optional<RenderLayer> outline = renderLayer.getAffectedOutline();
      if (outline.isPresent()) {
         this.outlineVertexConsumerProvider.setColor(color.getRGB() | 0xFF000000);
         return this.outlineVertexConsumerProvider.getBuffer(outline.get());
      } else {
         return vertexConsumerProvider.getBuffer(renderLayer);
      }
   }
}
