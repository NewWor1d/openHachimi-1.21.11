package pw.hachimi.client.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import org.joml.Vector4f;

/** 旧代码里 glBlitFramebuffer / Framebuffer.clear 的 1.21.11 等价物。 */
public final class OffscreenCopy {
   private OffscreenCopy() {
   }

   public static void copy(Framebuffer from, Framebuffer to) {
      if (from == null || to == null || from.getColorAttachment() == null || to.getColorAttachment() == null) {
         return;
      }
      int w = Math.min(from.textureWidth, to.textureWidth);
      int h = Math.min(from.textureHeight, to.textureHeight);
      if (w <= 0 || h <= 0) {
         return;
      }
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(from.getColorAttachment(), to.getColorAttachment(), 0, 0, 0, 0, 0, w, h);
   }

   public static void clear(Framebuffer framebuffer) {
      if (framebuffer == null || framebuffer.getColorAttachment() == null) {
         return;
      }
      RenderSystem.getDevice().createCommandEncoder().clearColorTexture(framebuffer.getColorAttachment(), 0);
   }
}
