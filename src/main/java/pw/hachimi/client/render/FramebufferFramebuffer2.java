package pw.hachimi.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import pw.hachimi.client.Mc;

/**
 * 1.21.11 迁移：旧的 GL FBO 直接被替换成 SimpleFramebuffer（GpuTexture 支撑）。
 *
 * <p>旧实现做法是 "把主帧缓冲 blit 到离屏 FBO → 在离屏里画 → 再 blit 回主帧缓冲"。
 * 新 API 用 CommandEncoder.copyTextureToTexture 完成同样的搬运，绘制则通过
 * CompatRender.setTarget(...) 把即时绘制输出重定向到离屏 Framebuffer。
 */
public class FramebufferFramebuffer2 extends SimpleFramebuffer {
   private static final FramebufferFramebuffer2 a_ = new FramebufferFramebuffer2();

   public FramebufferFramebuffer2() {
      super("hachimi_offscreen", 1, 1, true);
   }

   public static void afe(Runnable runnable) {
      aff(Mc.client.getFramebuffer(), runnable);
   }

   public static void aff(Framebuffer framebuffer, Runnable runnable) {
      RenderSystem.assertOnRenderThread();
      a_.resize(framebuffer.textureWidth, framebuffer.textureHeight, false);
      OffscreenCopy.copy(framebuffer, a_);
      CompatRender.setTarget(a_);
      try {
         runnable.run();
      } finally {
         CompatRender.clearTarget();
      }
      OffscreenCopy.copy(a_, framebuffer);
      OffscreenCopy.clear(a_);
   }

   public void resize(int width, int height, boolean getError) {
      if (this.textureWidth != width || this.textureHeight != height) {
         this.resize(width, height);
      }
   }
}
