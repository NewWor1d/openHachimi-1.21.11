package pw.hachimi.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextRenderer.class)
public interface TextRendererMixin {
    @Invoker("draw")
    void hookDrawLayer(String text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                       VertexConsumerProvider vertexConsumers, TextLayerType layerType,
                       int backgroundColor, int light);
}
