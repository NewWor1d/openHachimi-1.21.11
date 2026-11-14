package pw.hachimi.client.render;


import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * 1.21.11 即时渲染兼容层。
 *
 * <p>1.21.9 起 BufferRenderer / RenderSystem.setShader* 被删除，绘制改成
 * "RenderPipeline + RenderLayer.draw(BuiltBuffer)"。旧代码的写法是：
 *
 * <pre>
 * *   BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
 *   ... bb.vertex(...).color(...) ...
 *   CompatRender.draw(bb.end());
 * </pre>
 *
 * 迁移后的等价写法：
 *
 * <pre>
 *   BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
 *   ... bb.vertex(...).color(...) ...
 *   CompatRender.draw(bb.end());
 * </pre>
 *
 * 管线按 (顶点格式, 绘制模式, 混合, 深度测试) 自动选择，纹理用
 * {@link #setTexture(Identifier)} 指定，等价于旧的 CompatRender.setShaderTexture(0, id)。
 */
public final class CompatRender {
   private static final Map<String, RenderPipeline> PIPELINES = new HashMap<>();
   private static final Map<String, RenderLayer> LAYERS = new HashMap<>();

   private static Identifier texture;
   private static net.minecraft.client.gl.Framebuffer target;
   private static com.mojang.blaze3d.textures.GpuTextureView previousColorOverride;
   private static com.mojang.blaze3d.textures.GpuTextureView previousDepthOverride;
   private static net.minecraft.client.gui.DrawContext currentContext;
   private static boolean guiPhase;
   private static boolean blend = true;
   private static boolean depthTest = false;
   private static boolean depthWrite = false;
   private static float tintR = 1.0F;
   private static float tintG = 1.0F;
   private static float tintB = 1.0F;
   private static float tintA = 1.0F;

   private CompatRender() {
   }

   /** 在客户端启动、资源重载之前调用，把所有管线注册进 RenderPipelines，保证着色器预编译通过。 */
   public static void bootstrap() {
      for (VertexFormat format : new VertexFormat[]{
            VertexFormats.POSITION_COLOR,
            VertexFormats.POSITION_TEXTURE,
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL}) {
         for (DrawMode mode : new DrawMode[]{
               DrawMode.QUADS, DrawMode.DEBUG_LINES, DrawMode.DEBUG_LINE_STRIP, DrawMode.LINES,
               DrawMode.TRIANGLE_FAN, DrawMode.TRIANGLES}) {
            for (int flags = 0; flags < 4; flags++) {
               pipeline(format, mode, (flags & 1) != 0, (flags & 2) != 0);
            }
         }
      }
   }

   private static String key(VertexFormat format, DrawMode mode, boolean blendOn, boolean depthOn) {
      return format + "|" + mode + "|" + blendOn + "|" + depthOn;
   }

   private static RenderPipeline pipeline(VertexFormat format, DrawMode mode, boolean blendOn, boolean depthOn) {
      String k = key(format, mode, blendOn, depthOn);
      RenderPipeline cached = PIPELINES.get(k);
      if (cached != null) {
         return cached;
      }
      RenderPipeline.Snippet snippet = snippetFor(format);
      RenderPipeline.Builder builder = RenderPipeline.builder(snippet)
         .withLocation("hachimi/" + slug(format) + "_" + mode.name().toLowerCase() + (blendOn ? "_blend" : "_opaque") + (depthOn ? "_depth" : ""))
         .withVertexFormat(format, mode)
         .withCull(false);
      if (blendOn) {
         builder.withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT);
      } else {
         builder.withoutBlend();
      }
      builder.withDepthTestFunction(depthOn
         ? com.mojang.blaze3d.platform.DepthTestFunction.LEQUAL_DEPTH_TEST
         : com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST);
      builder.withDepthWrite(depthOn);
      RenderPipeline pipeline = builder.build();
      RenderPipelines.register(pipeline);
      PIPELINES.put(k, pipeline);
      return pipeline;
   }

   private static final RenderPipeline.Snippet POSITION_TEX_SNIPPET = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withVertexShader("core/position_tex")
      .withFragmentShader("core/position_tex")
      .withSampler("Sampler0")
      .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
      .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.QUADS)
      .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
      .buildSnippet();

   private static RenderPipeline.Snippet snippetFor(VertexFormat format) {
      if (format == VertexFormats.POSITION_TEXTURE) {
         return POSITION_TEX_SNIPPET;
      } else if (format == VertexFormats.POSITION_TEXTURE_COLOR) {
         return RenderPipelines.POSITION_TEX_COLOR_SNIPPET;
      } else if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
         return RenderPipelines.ENTITY_SNIPPET;
      } else {
         return RenderPipelines.POSITION_COLOR_SNIPPET;
      }
   }

   private static String slug(VertexFormat format) {
      // 1.21.11: Identifier 路径只允许 [a-z0-9/._-]，必须小写
      String s = format.toString().replaceAll("[^a-z0-9]", "_");
      return s.length() > 40 ? s.substring(0, 40) : s;
   }

   /** 世界渲染阶段直绘：不做 GUI 延迟提交，避免被当成 GUI 元素用正交投影绘制。 */
   public static void drawDirect(BuiltBuffer buffer) {
      BuiltBuffer.DrawParameters params = buffer.getDrawParameters();
      RenderPipeline pipeline = pipeline(params.format(), params.mode(), blend, depthTest);
      layer(pipeline, null).draw(buffer);
   }

   public static boolean isBlendEnabled() {
      return blend;
   }

   public static boolean isDepthTestEnabled() {
      return depthTest;
   }

   private static RenderLayer layer(RenderPipeline pipeline, Identifier tex) {
      String k = System.identityHashCode(pipeline) + "|" + tex + "|" + System.identityHashCode(target);
      RenderLayer layer = LAYERS.get(k);
      if (layer != null) {
         return layer;
      }
      RenderSetup.Builder builder = RenderSetup.builder(pipeline);
      if (tex != null) {
         builder.texture("Sampler0", tex);
      }
      if (target != null) {
         builder.outputTarget(new net.minecraft.client.render.OutputTarget("hachimi_output", () -> target));
      }
      layer = RenderLayer.of("hachimi_layer_" + LAYERS.size(), builder.build());
      LAYERS.put(k, layer);
      return layer;
   }

   /** 等价于旧的 framebuffer.beginWrite(...)：后续即时绘制输出到该 framebuffer。 */
   public static void setTarget(net.minecraft.client.gl.Framebuffer framebuffer) {
      target = framebuffer;
      // 1.21.11 里真正的全局输出重定向是这两个 override（RenderLayer#draw 会优先读取它们），
      // 只设置 RenderSetup.OutputTarget 管不到原版实体渲染 / renderHand 之类的 layer。
      previousColorOverride = com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride;
      previousDepthOverride = com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride;
      if (framebuffer != null
         && framebuffer != net.minecraft.client.MinecraftClient.getInstance().getFramebuffer()
         && framebuffer.getColorAttachmentView() != null) {
         com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride = framebuffer.getColorAttachmentView();
         com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride = framebuffer.getDepthAttachmentView();
      } else {
         com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride = null;
         com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride = null;
      }
   }

   public static void clearTarget() {
      target = null;
      com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride = previousColorOverride;
      com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride = previousDepthOverride;
      previousColorOverride = null;
      previousDepthOverride = null;
   }

   public static net.minecraft.client.gl.Framebuffer getTarget() {
      return target;
   }

   /**
    * 1.21.11 的 {@code DrawContext#getMatrices()} 是二维的 {@link org.joml.Matrix3x2fStack}，
    * 旧的 GUI 代码到处用 {@code getMatrices().peek().getPositionMatrix()} 取 Matrix4f。
    * 这里把二维变换提升成等价的 Matrix4f（z 方向不变）。
    */
   public static org.joml.Matrix4f guiMatrix4f(net.minecraft.client.gui.DrawContext context) {
      return guiMatrix4f(context.getMatrices());
   }

   public static org.joml.Matrix4f guiMatrix4f(org.joml.Matrix3x2fc m) {
      return new org.joml.Matrix4f(
         m.m00(), m.m01(), 0.0F, 0.0F,
         m.m10(), m.m11(), 0.0F, 0.0F,
         0.0F, 0.0F, 1.0F, 0.0F,
         m.m20(), m.m21(), 0.0F, 1.0F);
   }

   /** 把 GUI 的二维矩阵包装成旧的 MatrixStack，供仍以 MatrixStack 为参数的渲染工具复用。 */
   public static net.minecraft.client.util.math.MatrixStack asStack(org.joml.Matrix3x2fc m) {
      net.minecraft.client.util.math.MatrixStack stack = new net.minecraft.client.util.math.MatrixStack();
      stack.multiplyPositionMatrix(guiMatrix4f(m));
      return stack;
   }

   // ---- 旧 RenderSystem 状态的等价物 ----

   public static void setTexture(Identifier id) {
      texture = id;
   }

   /** 由 DrawContext 构造钩子每帧写入，GUI 元素提交需要它。 */
   public static void setContext(net.minecraft.client.gui.DrawContext context) {
      currentContext = context;
      guiPhase = false;
      batchSize = 0;
   }

   /** HUD 渲染阶段（InGameHud.render 到 GuiRenderer.render 之间），此阶段的即时绘制也要走 GuiRenderState。 */
   public static void setGuiPhase(boolean value) {
      guiPhase = value;
   }

   public static void enableBlend() {
      blend = true;
   }

   public static void disableBlend() {
      blend = false;
   }

   public static void enableDepthTest() {
      depthTest = true;
   }

   public static void disableDepthTest() {
      depthTest = false;
   }

   public static void depthMask(boolean write) {
      depthWrite = write;
   }

   public static void reset() {
      texture = null;
      blend = true;
      depthTest = false;
      depthWrite = false;
   }

   // ---- 旧 RenderSystem 上已删除的其它状态方法（1.21.9 起由 RenderPipeline 承载）----

   /** 颜色乘数。旧实现作用于全局着色器 uniform；新管线把颜色写在顶点里，这里保留给需要 tint 的调用点。 */
   public static void setShaderColor(float r, float g, float b, float a) {
      tintR = r;
      tintG = g;
      tintB = b;
      tintA = a;
   }

   public static float getTintR() {
      return tintR;
   }

   public static float getTintG() {
      return tintG;
   }

   public static float getTintB() {
      return tintB;
   }

   public static float getTintA() {
      return tintA;
   }

   public static void setShaderTexture(int unit, Identifier id) {
      if (unit == 0) {
         texture = id;
      }
   }

   public static void setShader(java.util.function.Supplier<?> program) {
      // 1.21.9 起着色器由 RenderPipeline 固定，无法在绘制前切换；保留调用点以免改动业务代码。
   }

   public static void setShader(Object program) {
      // 同上，兼容直接传入 ShaderProgram 对象的老调用点。
   }

   public static void defaultBlendFunc() {
   }

   public static void blendFunc(Object src, Object dst) {
   }

   public static void blendFuncSeparate(Object srcRgb, Object dstRgb, Object srcAlpha, Object dstAlpha) {
   }

   public static void enablePolygonOffset() {
   }

   public static void disablePolygonOffset() {
   }

   public static void polygonOffset(float factor, float units) {
   }

   public static void setShaderGlintAlpha(float alpha) {
   }

   public static void applyModelViewMatrix() {
   }

   public static void resetTextureMatrix() {
   }

   public static void disableCull() {
   }

   public static void enableScissor(int x, int y, int width, int height) {
      com.mojang.blaze3d.systems.RenderSystem.enableScissorForRenderTypeDraws(x, y, width, height);
   }

   public static void disableScissor() {
      com.mojang.blaze3d.systems.RenderSystem.disableScissorForRenderTypeDraws();
   }

   public static void setShaderFogStart(float start) {
   }

   public static void setShaderFogEnd(float end) {
   }

   public static void clearColor(float r, float g, float b, float a) {
      net.minecraft.client.gl.Framebuffer framebuffer = target != null
         ? target
         : net.minecraft.client.MinecraftClient.getInstance().getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachment() == null) {
         return;
      }
      int argb = ((int) (a * 255.0F) & 0xFF) << 24
         | ((int) (r * 255.0F) & 0xFF) << 16
         | ((int) (g * 255.0F) & 0xFF) << 8
         | ((int) (b * 255.0F) & 0xFF);
      com.mojang.blaze3d.systems.RenderSystem.getDevice()
         .createCommandEncoder()
         .clearColorTexture(framebuffer.getColorAttachment(), argb);
   }

   public static void texParameter(int target, int pname, int param) {
      com.mojang.blaze3d.opengl.GlStateManager._texParameter(target, pname, param);
   }

   /** 取代 CompatRender.draw(BuiltBuffer)。 */
   private static final java.util.List<BuiltBuffer> PENDING_BUFFERS = new java.util.ArrayList<>();
   private static final java.util.List<RenderLayer> PENDING_LAYERS = new java.util.ArrayList<>();

   public static void draw(BuiltBuffer buffer) {
      BuiltBuffer.DrawParameters params = buffer.getDrawParameters();
      VertexFormat format = params.format();
      boolean needsTexture = format == VertexFormats.POSITION_TEXTURE
         || format == VertexFormats.POSITION_TEXTURE_COLOR
         || format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
      RenderPipeline pipeline = pipeline(format, params.mode(), blend, depthTest);
      RenderLayer layer = layer(pipeline, needsTexture ? texture : null);

      // 1.21.11 的 GUI 是延迟提交的：Screen.render 阶段立即绘制的内容会被
      // GuiRenderer 随后画的状态覆盖。界面阶段直接把顶点包装成 GuiRenderer 的元素，
      // 随 GUI pass 一起用正确的正交投影绘制。
      if (target == null && shouldDeferGui()) {
         tickGuiElementLog();
         if (submitGuiElement(buffer, format, params)) {
            return;
         }
      }

      layer.draw(buffer);
   }

   private static boolean shouldDeferGui() {
      if (guiPhase) {
         return true;
      }
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      return client != null && client.currentScreen != null;
   }

   /** 由 GuiRenderer.render 的 TAIL 钩子调用，把本帧界面阶段缓存的绘制补上。 */
   private static final org.slf4j.Logger GUI_ELEMENT_LOG = org.slf4j.LoggerFactory.getLogger("Hachimi/GuiElement");
   private static long guiElementLogAt;
   private static int guiElementSubmitted;
   private static int guiElementRejected;
   private static String guiElementRejectReason = "";

   private static void tickGuiElementLog() {
      long now = System.currentTimeMillis();
      if (now - guiElementLogAt < 1000L) {
         return;
      }

      if (guiElementLogAt != 0L && (guiElementSubmitted != 0 || guiElementRejected != 0)) {
         GUI_ELEMENT_LOG.info(
            "gui elements/s: submitted={}, rejected={} ({})", guiElementSubmitted, guiElementRejected, guiElementRejectReason
         );
      }

      guiElementLogAt = now;
      guiElementSubmitted = 0;
      guiElementRejected = 0;
      guiElementRejectReason = "";
   }

   private static boolean rejectGuiElement(BuiltBuffer buffer, String reason) {
      guiElementRejected++;
      guiElementRejectReason = reason;
      return false;
   }

   private static boolean submitGuiElement(BuiltBuffer buffer, VertexFormat format, BuiltBuffer.DrawParameters params) {
      net.minecraft.client.gui.DrawContext context = currentContext;
      if (context == null) {
         return rejectGuiElement(buffer, "no-context");
      }
      if (params.mode() != DrawMode.QUADS) {
         return rejectGuiElement(buffer, "mode=" + params.mode());
      }

      RenderPipeline guiPipeline;
      boolean hasTexture;
      boolean hasColor;
      if (format == VertexFormats.POSITION_COLOR) {
         guiPipeline = net.minecraft.client.gl.RenderPipelines.GUI;
         hasTexture = false;
         hasColor = true;
      } else if (format == VertexFormats.POSITION_TEXTURE_COLOR) {
         guiPipeline = net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED;
         hasTexture = true;
         hasColor = true;
      } else if (format == VertexFormats.POSITION_TEXTURE) {
         // 无颜色元素：转成 GUI_TEXTURED，补白色
         guiPipeline = net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED;
         hasTexture = true;
         hasColor = false;
      } else {
         return rejectGuiElement(buffer, "format=" + format);
      }

      net.minecraft.client.texture.TextureSetup textureSetup = net.minecraft.client.texture.TextureSetup.empty();
      if (hasTexture) {
         if (texture == null) {
            guiElementRejected++;
            guiElementRejectReason = "no-texture";
            buffer.close();
            return true;
         }

         net.minecraft.client.texture.AbstractTexture abstractTexture =
            net.minecraft.client.MinecraftClient.getInstance().getTextureManager().getTexture(texture);
         if (abstractTexture == null || abstractTexture.getGlTextureView() == null) {
            guiElementRejected++;
            guiElementRejectReason = "texture-view-null:" + texture;
            buffer.close();
            return true;
         }

         textureSetup = net.minecraft.client.texture.TextureSetup.of(abstractTexture.getGlTextureView(), abstractTexture.getSampler());
      }

      int vertexCount = params.vertexCount();
      int stride = 3 + (hasTexture ? 2 : 0) + (hasColor ? 1 : 0);
      float[] data = new float[vertexCount * stride];

      java.nio.ByteBuffer bytes = buffer.getBuffer();
      bytes.order(java.nio.ByteOrder.nativeOrder());
      int base = bytes.position();
      int floatStride = 3 + (hasTexture ? 2 : 0) + (hasColor ? 1 : 0);
      for (int i = 0; i < vertexCount; i++) {
         int offset = base + i * floatStride * 4;
         int out = i * stride;
         data[out] = bytes.getFloat(offset);
         data[out + 1] = bytes.getFloat(offset + 4);
         data[out + 2] = bytes.getFloat(offset + 8);
         if (hasTexture) {
            data[out + 3] = bytes.getFloat(offset + 12);
            data[out + 4] = bytes.getFloat(offset + 16);
         }
         if (hasColor) {
            int colorOffset = offset + (hasTexture ? 20 : 12);
            // 内存里是 R,G,B,A 四个字节（VertexConsumer#color(int) 的写入顺序）；
            // 直接 getInt 会按本机字节序得到 A,B,G,R，必须逐字节还原成 ARGB，
            // 再用 intBitsToFloat 存进 float 数组，避免 int→float 丢精度。
            int r = bytes.get(colorOffset) & 0xFF;
            int g = bytes.get(colorOffset + 1) & 0xFF;
            int b = bytes.get(colorOffset + 2) & 0xFF;
            int a = bytes.get(colorOffset + 3) & 0xFF;
            int argb = a << 24 | r << 16 | g << 8 | b;
            data[out + stride - 1] = Float.intBitsToFloat(argb);
         }
      }

      buffer.close();
      appendGuiBatch(context, guiPipeline, textureSetup, data, hasTexture, hasColor);
      guiElementSubmitted++;
      return true;
   }

   // ---- GUI 元素合并：把连续的、同 pipeline + 同纹理的四边形累积到一个元素里， ----
   // ---- 元素数从每帧上千降到几十，显著降低分配与 GC 压力。                      ----
   private static RenderPipeline batchPipeline;
   private static net.minecraft.client.texture.TextureSetup batchTexture;
   private static boolean batchHasTexture;
   private static boolean batchHasColor;
   private static float[] batchData = new float[8192];
   private static int batchSize;

   private static void appendGuiBatch(
      net.minecraft.client.gui.DrawContext context,
      RenderPipeline pipeline,
      net.minecraft.client.texture.TextureSetup textureSetup,
      float[] data,
      boolean hasTexture,
      boolean hasColor
   ) {
      if (batchSize > 0
         && (batchPipeline != pipeline || batchTexture != textureSetup || batchHasTexture != hasTexture || batchHasColor != hasColor)) {
         flushGuiBatch(context);
      }

      batchPipeline = pipeline;
      batchTexture = textureSetup;
      batchHasTexture = hasTexture;
      batchHasColor = hasColor;

      if (batchSize + data.length > batchData.length) {
         float[] grown = new float[Math.max(batchData.length * 2, batchSize + data.length)];
         System.arraycopy(batchData, 0, grown, 0, batchSize);
         batchData = grown;
      }

      System.arraycopy(data, 0, batchData, batchSize, data.length);
      batchSize += data.length;
   }

   public static void flushGuiBatch() {
      flushGuiBatch(currentContext);
   }

   private static void flushGuiBatch(net.minecraft.client.gui.DrawContext context) {
      if (batchSize == 0 || context == null) {
         batchSize = 0;
         return;
      }

      float[] copy = java.util.Arrays.copyOf(batchData, batchSize);
      context.state.addSimpleElement(new HachimiGuiElement(batchPipeline, batchTexture, copy, batchHasTexture, batchHasColor));
      batchSize = 0;
   }

   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Hachimi/GuiDefer");
   private static boolean flushErrorLogged;

   private static final net.minecraft.client.render.ProjectionMatrix2 GUI_PROJECTION =
      new net.minecraft.client.render.ProjectionMatrix2("hachimi_gui_flush", 1000.0F, 11000.0F, true);

   public static void flushDeferred() {
      if (PENDING_BUFFERS.isEmpty()) {
         return;
      }

      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      net.minecraft.client.util.Window window = client.getWindow();
      com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
      com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
         GUI_PROJECTION.set((float)window.getFramebufferWidth() / window.getScaleFactor(), (float)window.getFramebufferHeight() / window.getScaleFactor()),
         com.mojang.blaze3d.systems.ProjectionType.ORTHOGRAPHIC
      );

      org.joml.Matrix4fStack modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.identity();

      int drawn = 0;
      try {
         for (int i = 0; i < PENDING_BUFFERS.size(); i++) {
            try {
               PENDING_LAYERS.get(i).draw(PENDING_BUFFERS.get(i));
               drawn++;
            } catch (Throwable throwable) {
               if (!flushErrorLogged) {
                  flushErrorLogged = true;
                  LOGGER.error("deferred GUI draw failed (layer={})", PENDING_LAYERS.get(i), throwable);
               }
            }
         }
      } finally {
         modelView.popMatrix();
         com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
      }

      if (!flushErrorLogged) {
         LOGGER.info("deferred GUI flush: {} draws", drawn);
      }

      PENDING_BUFFERS.clear();
      PENDING_LAYERS.clear();
   }

   public static RenderLayer texLayer(Identifier id) {
      return layer(pipeline(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS, true, false), id);
   }
}
