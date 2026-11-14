package pw.hachimi.satin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

/**
 * 1.21.11 后处理运行时（Satin 的替代实现）。
 *
 * <p>旧版 Satin 依赖 {@code PostEffectProcessor} 的 {@code PostEffectPass#getProgram} +
 * {@code GlUniform#set}，这套接口在 1.21.11 已被删除。这里不复用原版
 * {@code PostEffectProcessor}（它的 uniform 在解析时就固化进 UBO，无法每帧改），
 * 而是直接使用 {@code RenderPipeline} + {@code RenderPass} 自建整屏三角形绘制：
 *
 * <ul>
 *   <li>每个 pass 对应一个 {@link RenderPipeline}（顶点格式为空、TRIANGLES）；</li>
 *   <li>uniform 以 std140 UBO 形式绑定，每帧写入 {@code USAGE_UNIFORM|USAGE_COPY_DST} 缓冲；</li>
 *   <li>输入既可以是内部 target、主 framebuffer、外部 framebuffer（离屏场景），
 *       也可以是外部注册的动态纹理（用户 shader.png）。</li>
 * </ul>
 *
 * <p>JSON 格式（{@code assets/<ns>/shaders/post/*.json}）：
 *
 * <pre>
 * {
 *   "targets": ["swap"],
 *   "passes": [{
 *     "vertex_shader": "hachimiclient:post/screenquad",
 *     "fragment_shader": "hachimiclient:post/blur",
 *     "inputs": [{"sampler": "Diffuse", "source": "scene"},
 *                {"sampler": "Image",   "source": "texture:user"}],
 *     "output": "target:swap",
 *     "block": "BlurConfig",
 *     "uniforms": [{"name": "texelSize", "type": "vec2", "value": [1.0, 1.0]},
 *                  {"name": "radius",    "type": "float", "value": 4.0}]
 *   }]
 * }
 * </pre>
 *
 * <p>source 取值：{@code main} / {@code scene} / {@code target:<name>} / {@code texture:<name>}；
 * output 取值：{@code main} / {@code target:<name>}。
 */
public final class PostEffect implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();

   private final Identifier id;
   private final List<Pass> passes = new ArrayList<>();
   private final Map<String, SimpleFramebuffer> targets = new LinkedHashMap<>();
   private final Map<String, GpuTextureView> textureInputs = new HashMap<>();
   private final Map<String, UniformDef> uniforms = new HashMap<>();
   private final Map<String, float[]> values = new HashMap<>();

   private Framebuffer scene;
   private int width = -1;
   private int height = -1;
   private boolean failed;

   private PostEffect(Identifier id) {
      this.id = id;
   }

   public static PostEffect load(ResourceFactory factory, Identifier id) throws IOException {
      PostEffect effect = new PostEffect(id);
      Identifier file = id.withPath(path -> path.endsWith(".json") ? path : path + ".json");
      Resource resource = factory.getResource(file).orElse(null);
      if (resource == null) {
         throw new IOException("Post effect json not found: " + file);
      }
      try (Reader reader = resource.getReader()) {
         effect.read(JsonParser.parseReader(reader).getAsJsonObject());
      }
      return effect;
   }

   private void read(JsonObject root) throws IOException {
      JsonArray targetList = root.getAsJsonArray("targets");
      if (targetList != null) {
         for (JsonElement element : targetList) {
            this.targets.put(element.getAsString(), null);
         }
      }

      JsonArray passList = root.getAsJsonArray("passes");
      if (passList == null) {
         return;
      }

      int index = 0;
      for (JsonElement element : passList) {
         JsonObject object = element.getAsJsonObject();
         String vertex = object.get("vertex_shader").getAsString();
         String fragment = object.get("fragment_shader").getAsString();
         String output = object.get("output").getAsString();
         String block = object.has("block") ? object.get("block").getAsString() : null;
         boolean blend = object.has("blend") && object.get("blend").getAsBoolean();

         List<Input> inputs = new ArrayList<>();
         JsonArray inputArray = object.getAsJsonArray("inputs");
         if (inputArray != null) {
            for (JsonElement inputElement : inputArray) {
               JsonObject inputObject = inputElement.getAsJsonObject();
               inputs.add(new Input(
                  inputObject.get("sampler").getAsString(),
                  inputObject.get("source").getAsString(),
                  inputObject.has("bilinear") && inputObject.get("bilinear").getAsBoolean()
               ));
            }
         }

         List<UniformDef> defs = new ArrayList<>();
         JsonArray uniformArray = object.getAsJsonArray("uniforms");
         if (uniformArray != null) {
            for (JsonElement uniformElement : uniformArray) {
               JsonObject uniformObject = uniformElement.getAsJsonObject();
               String name = uniformObject.get("name").getAsString();
               String type = uniformObject.get("type").getAsString();
               int count = typeCount(type);
               float[] value = new float[count];
               JsonElement defaultElement = uniformObject.get("value");
               if (defaultElement != null && defaultElement.isJsonArray()) {
                  JsonArray defaultArray = defaultElement.getAsJsonArray();
                  for (int i = 0; i < Math.min(count, defaultArray.size()); i++) {
                     value[i] = defaultArray.get(i).getAsFloat();
                  }
               } else if (defaultElement != null && defaultElement.isJsonPrimitive()) {
                  value[0] = defaultElement.getAsFloat();
               }
               UniformDef def = new UniformDef(name, type, block, value);
               defs.add(def);
               this.uniforms.putIfAbsent(name, def);
               this.values.putIfAbsent(name, value.clone());
            }
         }

         RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
            .withLocation(this.id.withSuffixedPath("/" + index++))
            .withVertexShader(Identifier.of(vertex))
            .withFragmentShader(Identifier.of(fragment));
         for (Input input : inputs) {
            builder.withSampler(input.sampler + "Sampler");
         }
         if (block != null) {
            builder.withUniform(block, UniformType.UNIFORM_BUFFER);
         }
         if (blend) {
            builder.withBlend(BlendFunction.TRANSLUCENT);
         } else {
            builder.withoutBlend();
         }

         Pass pass = new Pass(builder.build(), inputs, output, block, defs, blend);
         if (block != null) {
            pass.createBuffer();
         }
         this.passes.add(pass);
      }
   }

   private static int typeCount(String type) {
      return switch (type) {
         case "float", "int" -> 1;
         case "vec2", "ivec2" -> 2;
         case "vec3", "ivec3" -> 3;
         case "vec4", "ivec4" -> 4;
         case "matrix4x4" -> 16;
         default -> 1;
      };
   }

   public Identifier getId() {
      return this.id;
   }

   public boolean isFailed() {
      return this.failed;
   }

   /** 外部离屏场景（VY 里画进 FramebufferFramebuffer3 的那些实体）。 */
   public void setScene(Framebuffer framebuffer) {
      this.scene = framebuffer;
   }

   /** 用户图片之类的动态纹理。 */
   public void setTexture(String name, GpuTextureView view) {
      if (view == null) {
         this.textureInputs.remove(name);
      } else {
         this.textureInputs.put(name, view);
      }
   }

   public void setUniform(String name, float[] value) {
      UniformDef def = this.uniforms.get(name);
      if (def == null) {
         return;
      }
      float[] target = this.values.get(name);
      if (target == null || target.length != value.length) {
         this.values.put(name, value.clone());
      } else {
         System.arraycopy(value, 0, target, 0, value.length);
      }
   }

   public float[] getUniform(String name) {
      return this.values.get(name);
   }

   public Framebuffer getFramebuffer(String name) {
      return this.targets.get(name);
   }

   /** 按当前窗口尺寸创建/缩放内部 target。 */
   public void setup(int framebufferWidth, int framebufferHeight) {
      if (framebufferWidth <= 0 || framebufferHeight <= 0) {
         return;
      }
      if (this.width == framebufferWidth && this.height == framebufferHeight) {
         return;
      }
      this.width = framebufferWidth;
      this.height = framebufferHeight;

      for (String name : this.targets.keySet()) {
         SimpleFramebuffer framebuffer = this.targets.get(name);
         if (framebuffer == null) {
            framebuffer = new SimpleFramebuffer("hachimi post " + this.id.getPath() + "/" + name, framebufferWidth, framebufferHeight, false);
            this.targets.put(name, framebuffer);
         } else {
            framebuffer.resize(framebufferWidth, framebufferHeight);
         }
      }
   }

   /** 执行所有 pass；调用方负责把最终 target 合成回主 framebuffer（menu 除外，它在 JSON 里自己输出到 main）。 */
   public void render(float tickDelta) {
      if (this.failed || this.passes.isEmpty()) {
         return;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      Framebuffer main = client.getFramebuffer();
      if (main == null) {
         return;
      }
      this.setup(main.textureWidth, main.textureHeight);
      if (this.width <= 0) {
         return;
      }

      try {
         for (Pass pass : this.passes) {
            this.runPass(pass);
         }
      } catch (Throwable throwable) {
         this.failed = true;
         LOGGER.error("Hachimi post effect {} failed; disabling it", this.id, throwable);
      }
   }

   private void runPass(Pass pass) {
      Framebuffer output = this.resolveFramebuffer(pass.output);
      if (output == null || output.getColorAttachmentView() == null) {
         return;
      }

      if (pass.block != null && pass.buffer == null) {
         pass.createBuffer();
      }
      if (pass.block != null && pass.buffer != null) {
         try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, pass.bufferSize);
            for (UniformDef def : pass.uniforms) {
               float[] value = this.values.get(def.name);
               if (value == null) {
                  value = def.defaultValue;
               }
               writeUniform(builder, def.type, value);
            }
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(pass.buffer.slice(), builder.get());
         }
      }

      try (RenderPass renderPass = RenderSystem.getDevice()
         .createCommandEncoder()
         .createRenderPass(() -> "hachimi post " + this.id, output.getColorAttachmentView(), OptionalInt.empty())) {
         renderPass.setPipeline(pass.pipeline);
         if (pass.block != null && pass.buffer != null) {
            renderPass.setUniform(pass.block, pass.buffer.slice());
         }

         for (Input input : pass.inputs) {
            GpuTextureView view = this.resolveTexture(input);
            if (view == null) {
               // 未绑定的 sampler 不能留空，否则 RenderPass 可能报错；退回主 framebuffer。
               Framebuffer fallback = MinecraftClient.getInstance().getFramebuffer();
               view = fallback == null ? null : fallback.getColorAttachmentView();
            }
            if (view == null) {
               continue;
            }
            renderPass.bindTexture(
               input.sampler + "Sampler",
               view,
               RenderSystem.getSamplerCache().get(input.bilinear ? FilterMode.LINEAR : FilterMode.NEAREST)
            );
         }

         renderPass.draw(0, 3);
      }
   }

   private static void writeUniform(Std140Builder builder, String type, float[] value) {
      switch (type) {
         case "int" -> builder.putInt((int) at(value, 0));
         case "ivec2" -> builder.putIVec2((int) at(value, 0), (int) at(value, 1));
         case "ivec3" -> builder.putIVec3((int) at(value, 0), (int) at(value, 1), (int) at(value, 2));
         case "ivec4" -> builder.putIVec4((int) at(value, 0), (int) at(value, 1), (int) at(value, 2), (int) at(value, 3));
         case "vec2" -> builder.putVec2(at(value, 0), at(value, 1));
         case "vec3" -> builder.putVec3(at(value, 0), at(value, 1), at(value, 2));
         case "vec4" -> builder.putVec4(at(value, 0), at(value, 1), at(value, 2), at(value, 3));
         case "matrix4x4" -> builder.putMat4f(new org.joml.Matrix4f(
            at(value, 0), at(value, 1), at(value, 2), at(value, 3),
            at(value, 4), at(value, 5), at(value, 6), at(value, 7),
            at(value, 8), at(value, 9), at(value, 10), at(value, 11),
            at(value, 12), at(value, 13), at(value, 14), at(value, 15)
         ));
         default -> builder.putFloat(at(value, 0));
      }
   }

   private static float at(float[] value, int index) {
      return value != null && index < value.length ? value[index] : 0.0F;
   }

   private Framebuffer resolveFramebuffer(String source) {
      if ("main".equals(source)) {
         return MinecraftClient.getInstance().getFramebuffer();
      }
      if ("scene".equals(source)) {
         return this.scene;
      }
      if (source.startsWith("target:")) {
         return this.targets.get(source.substring("target:".length()));
      }
      return null;
   }

   private GpuTextureView resolveTexture(Input input) {
      String source = input.source;
      if ("main".equals(source)) {
         Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
         return main == null ? null : main.getColorAttachmentView();
      }
      if ("scene".equals(source)) {
         return this.scene == null ? null : this.scene.getColorAttachmentView();
      }
      if (source.startsWith("target:")) {
         Framebuffer framebuffer = this.targets.get(source.substring("target:".length()));
         return framebuffer == null ? null : framebuffer.getColorAttachmentView();
      }
      if (source.startsWith("texture:")) {
         return this.textureInputs.get(source.substring("texture:".length()));
      }
      return null;
   }

   @Override
   public void close() {
      for (SimpleFramebuffer framebuffer : this.targets.values()) {
         if (framebuffer != null) {
            framebuffer.delete();
         }
      }
      this.targets.replaceAll((name, framebuffer) -> null);
      this.width = -1;
      this.height = -1;
      for (Pass pass : this.passes) {
         pass.close();
      }
   }

   private static final class Input {
      final String sampler;
      final String source;
      final boolean bilinear;

      Input(String sampler, String source, boolean bilinear) {
         this.sampler = sampler;
         this.source = source;
         this.bilinear = bilinear;
      }
   }

   private record UniformDef(String name, String type, String block, float[] defaultValue) {
   }

   private static final class Pass {
      final RenderPipeline pipeline;
      final List<Input> inputs;
      final String output;
      final String block;
      final List<UniformDef> uniforms;
      final boolean blend;

      GpuBuffer buffer;
      int bufferSize;

      Pass(RenderPipeline pipeline, List<Input> inputs, String output, String block, List<UniformDef> uniforms, boolean blend) {
         this.pipeline = pipeline;
         this.inputs = inputs;
         this.output = output;
         this.block = block;
         this.uniforms = uniforms;
         this.blend = blend;
      }

      void createBuffer() {
         if (RenderSystem.getDevice() == null) {
            return;
         }
         Std140SizeCalculator calculator = new Std140SizeCalculator();
         for (UniformDef def : this.uniforms) {
            switch (def.type) {
               case "int" -> calculator.putInt();
               case "ivec2" -> calculator.putIVec2();
               case "ivec3" -> calculator.putIVec3();
               case "ivec4" -> calculator.putIVec4();
               case "vec2" -> calculator.putVec2();
               case "vec3" -> calculator.putVec3();
               case "vec4" -> calculator.putVec4();
               case "matrix4x4" -> calculator.putMat4f();
               default -> calculator.putFloat();
            }
         }
         int size = Math.max(16, (calculator.get() + 15) & ~15);
         this.bufferSize = size;
         this.buffer = RenderSystem.getDevice()
            .createBuffer(
               () -> "hachimi post uniform " + this.block,
               GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
               size
            );
      }

      void close() {
         if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
         }
      }
   }
}
