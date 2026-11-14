package pw.hachimi.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public class FramebufferFramebuffer extends SimpleFramebuffer {
   private static final Map<Integer, FramebufferFramebuffer> INSTANCES = new HashMap();
   public static int e;

   public FramebufferFramebuffer(int samples) {
      super("hachimi_blur_" + samples, 1, 1, true);
   }

   public static FramebufferFramebuffer adO(int n) {
      return INSTANCES.computeIfAbsent(n, FramebufferFramebuffer::new);
   }

   public static void adP(int n, Runnable runnable) {
      adQ(Math.min(16, Math.max(1, n)), MinecraftClient.getInstance().getFramebuffer(), runnable);
   }

   public static void adQ(int n, Framebuffer framebuffer, Runnable runnable) {
      RenderSystem.assertOnRenderThread();
      FramebufferFramebuffer offscreen = adO(n);
      offscreen.resize(framebuffer.textureWidth, framebuffer.textureHeight, false);
      OffscreenCopy.copy(framebuffer, offscreen);
      CompatRender.setTarget(offscreen);
      try {
         runnable.run();
      } finally {
         CompatRender.clearTarget();
      }
      OffscreenCopy.copy(offscreen, framebuffer);
      OffscreenCopy.clear(offscreen);
   }

   public void resize(int width, int height, boolean getError) {
      if (this.textureWidth != width || this.textureHeight != height) {
         this.resize(width, height);
      }
   }
}
