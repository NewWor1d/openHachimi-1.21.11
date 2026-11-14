package pw.hachimi.client.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.hachimi.client.utils.config.GlStateManagerFileUtils;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
   // 1.21.11: glShaderSource(int, String) —— 旧版是 List<String>
   @Inject(method = "glShaderSource", at = @At("HEAD"), cancellable = true)
   private static void readSource(int n, String source, CallbackInfo callbackInfo) {
      GlStateManagerFileUtils.avj(n, source, callbackInfo);
   }
}
