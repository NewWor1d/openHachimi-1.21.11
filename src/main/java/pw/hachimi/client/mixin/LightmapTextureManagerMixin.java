package pw.hachimi.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.awt.Color;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.event.UpdateEvent2;
import pw.hachimi.client.event.UpdateEvent3;
import pw.hachimi.eventbus.EventBusImpl;

/**
 * 1.21.11 的光照贴图不再是 NativeImage，而是由 {@code BILT_SCREEN_LIGHTMAP}
 * 着色器从 UBO 参数生成到一张 16×16 的 {@link GpuTexture}。
 *
 * <p>Fullbright 的 GAMMA/LIGHT 两种模式本质都是「把整张光照贴图填成纯色」，
 * 所以这里在 {@code update(float)} 返回后直接把整张纹理 clear 成目标颜色，
 * 取代旧版逐像素 {@code NativeImage#setColor} 的写法。
 */
@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
   @Shadow
   @Final
   private GpuTexture glTexture;

   @Inject(method = "update(F)V", at = @At("RETURN"))
   private void hachimi$overrideLightmap(float tickProgress, CallbackInfo callbackInfo) {
      int argb = 0;
      boolean override = false;

      UpdateEvent2 event2 = new UpdateEvent2();
      EventBusImpl.INSTANCE.rC(event2);
      if (event2.aik()) {
         Color color = event2.getColor();
         if (color != null) {
            argb = 0xFF000000 | color.getRGB();
            override = true;
         }
      } else {
         UpdateEvent3 event3 = new UpdateEvent3(-1);
         EventBusImpl.INSTANCE.rC(event3);
         if (event3.aik()) {
            argb = 0xFFFFFFFF;
            override = true;
         }
      }

      if (System.getProperty("hachimi.debug.whitelightmap") != null) {
         argb = 0xFFFFFFFF;
         override = true;
      }

      if (override && this.glTexture != null) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.glTexture, argb);
      }
   }
}
