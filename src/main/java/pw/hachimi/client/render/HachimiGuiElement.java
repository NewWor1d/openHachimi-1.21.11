package pw.hachimi.client.render;

import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.joml.Matrix3x2f;

/**
 * 1.21.11 GUI 改为「收集渲染状态 → GuiRenderer 统一绘制」两段式。
 * 客户端大量自绘代码走的是即时 BufferBuilder，这里把解析好的顶点包装成
 * GuiRenderer 能消费的元素，随 GUI pass 一起用正确的正交投影绘制。
 */
public final class HachimiGuiElement implements SimpleGuiElementRenderState {
   private static final Matrix3x2f IDENTITY = new Matrix3x2f();

   private final RenderPipeline pipeline;
   private final TextureSetup textures;
   private final float[] vertices;
   private final boolean hasTexture;
   private final boolean hasColor;
   private final ScreenRect bounds;

   public HachimiGuiElement(RenderPipeline pipeline, TextureSetup textures, float[] vertices, boolean hasTexture, boolean hasColor) {
      this.pipeline = pipeline;
      this.textures = textures;
      this.vertices = vertices;
      this.hasTexture = hasTexture;
      this.hasColor = hasColor;

      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      int stride = 3 + (hasTexture ? 2 : 0) + (hasColor ? 1 : 0);
      for (int i = 0; i < vertices.length; i += stride) {
         float x = vertices[i];
         float y = vertices[i + 1];
         minX = Math.min(minX, x);
         minY = Math.min(minY, y);
         maxX = Math.max(maxX, x);
         maxY = Math.max(maxY, y);
      }

      this.bounds = vertices.length == 0
         ? new ScreenRect(0, 0, 0, 0)
         : new ScreenRect((int)minX, (int)minY, Math.max(1, (int)(maxX - minX)), Math.max(1, (int)(maxY - minY)));
   }

   @Override
   public void setupVertices(VertexConsumer consumer) {
      int stride = 3 + (this.hasTexture ? 2 : 0) + (this.hasColor ? 1 : 0);
      for (int i = 0; i < this.vertices.length; i += stride) {
         float x = this.vertices[i];
         float y = this.vertices[i + 1];
         VertexConsumer vertex = consumer.vertex(IDENTITY, x, y);
         if (this.hasTexture) {
            vertex = vertex.texture(this.vertices[i + 3], this.vertices[i + 4]);
         }
         if (this.hasColor) {
            vertex = vertex.color(Float.floatToRawIntBits(this.vertices[i + stride - 1]));
         } else {
            vertex.color(-1);
         }
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return this.pipeline;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textures;
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }

   @Override
   public ScreenRect bounds() {
      return this.bounds;
   }
}
