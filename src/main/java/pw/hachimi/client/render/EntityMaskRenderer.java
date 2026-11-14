package pw.hachimi.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SequencedMap;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.util.BufferAllocator;

/**
 * 1.21.11 实体离屏重绘。
 *
 * <p>1.21.11 的实体渲染拆成了两步：{@code EntityRenderManager.render(state, ...)}
 * 只把绘制命令塞进 {@code OrderedRenderCommandQueue}，真正落笔由
 * {@code RenderDispatcher} + {@code VertexConsumerProvider.Immediate} 完成。
 * 因此想「把实体再画一遍到离屏 FBO」，必须自建一套 queue/dispatcher，
 * 并用 {@link RenderSystem#outputColorTextureOverride} 把 RenderLayer 的输出重定向到目标 FBO
 * （原版 GUI 里的实体预览也是这么做的）。
 */
public final class EntityMaskRenderer {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Hachimi/OffscreenEntities");
   private static boolean errorLogged;

   private static OrderedRenderCommandQueueImpl queue;
   private static RenderDispatcher dispatcher;
   private static VertexConsumerProvider.Immediate immediate;
   private static OutlineVertexConsumerProvider outlineVertexConsumers;

   private EntityMaskRenderer() {
   }

   /** 取得（必要时创建）本模组私有的命令队列；调用后即可向其中提交实体/方块实体渲染状态。 */
   public static OrderedRenderCommandQueueImpl begin() {
      if (queue == null) {
         MinecraftClient client = MinecraftClient.getInstance();
         queue = new OrderedRenderCommandQueueImpl();
         immediate = createImmediate();
         outlineVertexConsumers = new OutlineVertexConsumerProvider();
         dispatcher = new RenderDispatcher(
            queue,
            client.getBlockRenderManager(),
            immediate,
            client.getAtlasManager(),
            outlineVertexConsumers,
            VertexConsumerProvider.immediate(new BufferAllocator(1536)),
            client.textRenderer
         );
      }

      return queue;
   }

   /**
    * 必须和原版 {@code BufferBuilderStorage#entityVertexConsumers} 一样，把 glint / 实体贴图这些
    * RenderLayer 放进 layerBuffers。否则 {@code ItemRenderer#getItemGlintConsumer} 的
    * {@code VertexConsumers.union(glint, layer)} 里第一个 buffer 会在切换 layer 时被
    * Immediate 立刻 end 掉，union 继续写就会抛 {@code IllegalStateException: Not building!}。
    */
   private static VertexConsumerProvider.Immediate createImmediate() {
      SequencedMap<RenderLayer, BufferAllocator> layers = new Object2ObjectLinkedOpenHashMap<>();
      assign(layers, TexturedRenderLayers.getEntitySolid());
      assign(layers, TexturedRenderLayers.getEntityCutout());
      assign(layers, TexturedRenderLayers.getItemTranslucentCull());
      assign(layers, TexturedRenderLayers.getBlockTranslucentCull());
      assign(layers, TexturedRenderLayers.getShieldPatterns());
      assign(layers, TexturedRenderLayers.getBeds());
      assign(layers, TexturedRenderLayers.getShulkerBoxes());
      assign(layers, TexturedRenderLayers.getSign());
      assign(layers, TexturedRenderLayers.getHangingSign());
      layers.put(TexturedRenderLayers.getChest(), new BufferAllocator(786432));
      assign(layers, RenderLayers.armorEntityGlint());
      assign(layers, RenderLayers.glint());
      assign(layers, RenderLayers.glintTranslucent());
      assign(layers, RenderLayers.entityGlint());
      assign(layers, RenderLayers.waterMask());
      return VertexConsumerProvider.immediate(layers, new BufferAllocator(786432));
   }

   private static void assign(SequencedMap<RenderLayer, BufferAllocator> layers, RenderLayer layer) {
      layers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
   }

   /** 把队列里的命令真正画进目标 framebuffer（遮罩用途，会清一次深度）。 */
   public static void end(Framebuffer target) {
      draw(target, true);
   }

   /** 把队列里的命令画进目标 framebuffer，但不动目标已有的深度缓冲（nametags 的物品图标用）。 */
   public static void flush(Framebuffer target) {
      draw(target, false);
   }

   private static void draw(Framebuffer target, boolean clearDepth) {
      if (queue == null || target == null || target.getColorAttachmentView() == null) {
         return;
      }

      com.mojang.blaze3d.textures.GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
      com.mojang.blaze3d.textures.GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
      try {
         if (clearDepth && target.getDepthAttachment() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthAttachment(), 1.0);
         }
         RenderSystem.outputColorTextureOverride = target.getColorAttachmentView();
         RenderSystem.outputDepthTextureOverride = target.getDepthAttachmentView();
         dispatcher.render();
         outlineVertexConsumers.draw();
         immediate.draw();
      } catch (Throwable throwable) {
         if (!errorLogged) {
            errorLogged = true;
            LOGGER.error("Hachimi offscreen entity render failed", throwable);
         }
      } finally {
         RenderSystem.outputColorTextureOverride = previousColor;
         RenderSystem.outputDepthTextureOverride = previousDepth;
         queue.clear();
      }
   }
}
