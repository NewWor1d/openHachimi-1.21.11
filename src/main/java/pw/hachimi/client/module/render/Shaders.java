package pw.hachimi.client.module.render;

import pw.hachimi.client.render.CompatRender;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pw.hachimi.client.Bootstrap;
import pw.hachimi.client.event.EventPhase;
import pw.hachimi.client.event.OnGameJoinEvent;
import pw.hachimi.client.event.UnknownEvent3;
import pw.hachimi.client.event.render.HasOutlineEvent;
import pw.hachimi.client.event.render.RenderEvent3;
import pw.hachimi.client.event.render.RenderEvent7;
import pw.hachimi.client.event.render.RenderFirstPersonItem_2Event;
import pw.hachimi.client.event.render.RenderLabelIfPresentEvent;
import pw.hachimi.client.event.render.RenderPreEvent;
import pw.hachimi.client.event.render.RenderWorldEvent;
import pw.hachimi.client.event.render.RenderWorld_2Event;
import pw.hachimi.client.manager.FramebufferBuilderHelper;
import pw.hachimi.client.manager.Managers;
import pw.hachimi.client.manager.RenderUtils;
import pw.hachimi.client.mixin.GameRendererMixin;
import pw.hachimi.client.mixin.WorldRendererMixin;
import pw.hachimi.client.model.render.ShadersCustomBloomMode;
import pw.hachimi.client.model.render.ShadersDefaultGradientMode;
import pw.hachimi.client.module.ClientModule;
import pw.hachimi.client.module.ColorHelper;
import pw.hachimi.client.module.ModuleCategory;
import pw.hachimi.client.module.player.Freecam;
import pw.hachimi.client.setting.BooleanSetting;
import pw.hachimi.client.setting.ColorSetting;
import pw.hachimi.client.setting.EnumSetting;
import pw.hachimi.client.setting.NumberSetting;
import pw.hachimi.client.setting.Setting;
import pw.hachimi.client.utils.StopwatchImpl;
import pw.hachimi.client.utils.world.BoxEntityUtils;
import pw.hachimi.client.utils.world.CollisionUtils;
import pw.hachimi.eventbus.annotation.EventListener;
import pw.hachimi.satin.ManagedShaderEffect;

public class Shaders extends ClientModule {
   private static Shaders INSTANCE;
   Setting renderDistance = this.registerSetting(new NumberSetting("RenderDistance", "The shader render range", 0.0F, 50.0F, 200.0F));
   Setting texture = this.registerSetting(new BooleanSetting("Texture", "Renders the entity model texture", true));
   Setting outline = this.registerSetting(new EnumSetting("Outline", "Adds an outline around the shader", ShadersCustomBloomMode.Default, ShadersCustomBloomMode.values()));
   Setting syncColor = this.registerSetting(new BooleanSetting("SyncColor", "Use fill color", true, this::isSyncColorVisible));
   public Setting width = this.registerSetting(new NumberSetting("Width", "The outline width", 0.1F, 1.5F, 10.0F, this::isWidthVisible));
   public Setting fastOutline = this.registerSetting(new BooleanSetting("FastOutline", "Faster outline calculations", false, this::isFastOutlineVisible));
   public Setting quality = this.registerSetting(new NumberSetting("Quality", "The outline pixel quality", 2, 10, 32, this::isQualityVisible));
   public Setting steps = this.registerSetting(new NumberSetting("Steps", "The number of steps for finding edges", 2, 4, 32, this::isStepsVisible));
   public Setting glow = this.registerSetting(new BooleanSetting("Glow", "Glow outline", false, this::isGlowVisible));
   public Setting inner = this.registerSetting(new BooleanSetting("Inner", "Inner glow", false, this::isInnerVisible));
   public Setting glowFactor = this.registerSetting(new NumberSetting("GlowFactor", "The glow radius", 0.1F, 1.0F, 3.0F, this::isGlowFactorVisible));
   Setting fill = this.registerSetting(new EnumSetting("Fill", "The shader mode", ShadersDefaultGradientMode.DEFAULT, ShadersDefaultGradientMode.values()));
   Setting speed = this.registerSetting(new NumberSetting("Speed", "The gradient speed factor", 0.01F, 1.5F, 10.0F));
   Setting dots = this.registerSetting(new BooleanSetting("Dots", "Hacker esp", false));
   Setting dotRadius = this.registerSetting(new NumberSetting("DotRadius", "Width between the dots", 5, 8, 16, this::isDotRadiusVisible));
   Setting fillColor = this.registerSetting(createColorSetting("FillColor", "The gradient color of the shader", b_1(), true, this::isFillColorVisible));
   Setting factor = this.registerSetting(new NumberSetting("Factor", "The gradient factor", 0.1F, 1.0F, 2.0F, this::isFactorVisible));
   Setting saturation = this.registerSetting(new NumberSetting("Saturation", "Rainbow saturation", 0.0F, 1.0F, 1.0F, this::isSaturationVisible));
   Setting brightness = this.registerSetting(new NumberSetting("Brightness", "Rainbow brightness", 0.0F, 1.0F, 1.0F, this::isBrightnessVisible));
   Setting image = this.registerSetting(new BooleanSetting("Image", "Image shader", false));
   Setting mix = this.registerSetting(new BooleanSetting("Mix", "Mixes the image with the shader colors", true, this::isMixVisible));
   Setting mixFactor = this.registerSetting(new NumberSetting("MixFactor", "The mix image factor", 0.0F, 0.5F, 1.0F, this::isMixFactorVisible));
   Setting opacity = this.registerSetting(new NumberSetting("Opacity", "The transparency of the fill", 0.0F, 0.35F, 1.0F));
   Setting hands = this.registerSetting(new BooleanSetting("Hands", "Render shaders on first-person hands", true));
   Setting handsColor = this.registerSetting(createColorSetting("HandsColor", "The color of the shader", new Color(0, 100, 255), this::isHandsColorVisible));
   Setting self = this.registerSetting(new BooleanSetting("Self", "Render shaders on the player", true));
   Setting selfColor = this.registerSetting(createColorSetting("SelfColor", "The render color for self", new Color(200, 60, 60), this::isSelfColorVisible));
   Setting players = this.registerSetting(new BooleanSetting("Players", "Render shaders on other players", true));
   Setting friendsColor = this.registerSetting(new BooleanSetting("FriendsColor", "Render shaders on friends", true));
   Setting playersColor = this.registerSetting(createColorSetting("PlayersColor", "The render color for players", new Color(200, 60, 60), this::isPlayersColorVisible));
   Setting monsters = this.registerSetting(new BooleanSetting("Monsters", "Render shaders on monsters", true));
   Setting monstersColor = this.registerSetting(createColorSetting("MonstersColor", "The render color for monsters", new Color(200, 60, 60), this::isMonstersColorVisible));
   Setting animals = this.registerSetting(new BooleanSetting("Animals", "Render shaders on animals", true));
   Setting animalsColor = this.registerSetting(createColorSetting("AnimalsColor", "The render color for animals", new Color(0, 200, 0), this::isAnimalsColorVisible));
   Setting items = this.registerSetting(new BooleanSetting("Items", "Render shaders on items", true));
   Setting itemsColor = this.registerSetting(createColorSetting("ItemsColor", "The render color for items", new Color(200, 100, 0), this::isItemsColorVisible));
   Setting crystals = this.registerSetting(new BooleanSetting("Crystals", "Render shaders on crystals", true));
   Setting crystalsColor = this.registerSetting(createColorSetting("CrystalsColor", "The render color for end crystals", new Color(200, 100, 200), this::isCrystalsColorVisible));
   Setting projectiles = this.registerSetting(new BooleanSetting("Projectiles", "Render shaders on projectiles", true));
   Setting projectilesColor = this.registerSetting(createColorSetting("ProjectilesColor", "The render color for projectiles", new Color(200, 100, 200), this::isProjectilesColorVisible));
   Setting chests = this.registerSetting(new BooleanSetting("Chests", "Render chests through walls", false));
   Setting chestsColor = this.registerSetting(createColorSetting("ChestsColor", "The render color for chests", new Color(200, 200, 101), false, this::isChestsColorVisible));
   Setting enderChests = this.registerSetting(new BooleanSetting("EnderChests", "Render ender chests through walls", false));
   Setting enderChestsColor = this.registerSetting(createColorSetting("EnderChestsColor", "The render color for ender chests", new Color(155, 0, 200), false, this::isEnderChestsColorVisible));
   Setting shulkers = this.registerSetting(new BooleanSetting("Shulkers", "Render shulkers through walls", false));
   Setting shulkersColor = this.registerSetting(createColorSetting("ShulkersColor", "The render color for shulkers", new Color(200, 0, 106), false, this::isShulkersColorVisible));
   StopwatchImpl stopwatchImpl = new StopwatchImpl();
   private float Z;
   private int aa;
   private net.minecraft.client.texture.AbstractTexture shaderImage;
   public static boolean ab;

   public Shaders() {
      super("Shaders", "Renders shaders over entities", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static Shaders agJ() {
      return INSTANCE;
   }

   @Override
   public void awd() {
      if ((Boolean)this.image.atx()) {
         this.agV();
      }

      ab = false;
   }

   @Override
   public void awe() {
      ab = false;
   }

   @EventListener
   public void agK(UnknownEvent3 unknownEvent3) {
      if (client.player != null) {
         if (unknownEvent3.getSetting() == this.image && (Boolean)this.image.atx() && unknownEvent3.getEventPhase() == EventPhase.POST) {
            this.agV();
         }
      }
   }

   @EventListener
   public void onOnGameJoin(OnGameJoinEvent onGameJoinEvent) {
      if ((Boolean)this.image.atx()) {
         this.agV();
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent renderWorldEvent) {
      if (!((Float)this.renderDistance.atx() <= 0.0F)) {
         ManagedShaderEffect managedShaderEffect = Managers.framebufferBuilderHelper.getManagedShaderEffect();
         FramebufferBuilderHelper temp = Managers.framebufferBuilderHelper;
         ManagedShaderEffect temp2 = managedShaderEffect;
         ManagedShaderEffect temp3 = managedShaderEffect;
         final RenderWorldEvent renderWorldEvent2 = renderWorldEvent;
         final ManagedShaderEffect managedShaderEffect2 = temp3;
         final Shaders shaders2 = this;
         Runnable temp4 = new Runnable() {
            @Override
            public void run() {
               shaders2.ahd(managedShaderEffect2, renderWorldEvent2);
            }
         };
         final RenderWorldEvent renderWorldEvent3 = renderWorldEvent;
         final Shaders shaders3 = this;
         temp.VY(temp2, temp4, new Runnable() {
            @Override
            public void run() {
               shaders3.ahc(renderWorldEvent3);
            }
         });
      }
   }

   @EventListener
   public void onRenderWorld_2(RenderWorld_2Event renderWorld2Event) {
      if ((Boolean)this.hands.atx()) {
         ManagedShaderEffect managedShaderEffect = Managers.framebufferBuilderHelper.getManagedShaderEffect();
         FramebufferBuilderHelper temp = Managers.framebufferBuilderHelper;
         ManagedShaderEffect temp2 = managedShaderEffect;
         ManagedShaderEffect temp3 = managedShaderEffect;
         final RenderWorld_2Event renderWorld2Event2 = renderWorld2Event;
         final ManagedShaderEffect managedShaderEffect2 = temp3;
         final Shaders shaders2 = this;
         Runnable temp4 = new Runnable() {
            @Override
            public void run() {
               shaders2.ahb(managedShaderEffect2, renderWorld2Event2);
            }
         };
         final RenderWorld_2Event renderWorld2Event3 = renderWorld2Event;
         temp.VY(temp2, temp4, new Runnable() {
            @Override
            public void run() {
               Shaders.aha(renderWorld2Event3);
            }
         });
      }
   }

   private void agO(float f, ManagedShaderEffect managedShaderEffect) {
      this.agU(managedShaderEffect);
      managedShaderEffect.setUniformValue("resolution", client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
      managedShaderEffect.setUniformValue("texelSize", 1.0F / client.getWindow().getScaledWidth(), 1.0F / client.getWindow().getScaledHeight());
      managedShaderEffect.setUniformValue("samples", ((ShadersCustomBloomMode)this.outline.atx()).gs());
      managedShaderEffect.setUniformValue("steps", ((ShadersCustomBloomMode)this.outline.atx()).gt());
      byte b;
      if (((ShadersCustomBloomMode)this.outline.atx()).gr()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("fastOutline", b);
      float f2;
      if (((ShadersCustomBloomMode)this.outline.atx()).gq()) {
         if (((ShadersCustomBloomMode)this.outline.atx()).gr()) {
            f2 = 0.3F;
         } else {
            f2 = 1.0F;
         }

         f2 = Math.max(f2, ((ShadersCustomBloomMode)this.outline.atx()).gx());
      } else {
         f2 = 0.0F;
      }

      managedShaderEffect.setUniformValue("radius", f2);
      if (((ShadersCustomBloomMode)this.outline.atx()).gu()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("glow", b);
      if (((ShadersCustomBloomMode)this.outline.atx()).gv()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("inner", b);
      managedShaderEffect.setUniformValue("glowRadius", ((ShadersCustomBloomMode)this.outline.atx()).gw());
      managedShaderEffect.setUniformValue("time", this.Z);
      managedShaderEffect.setUniformValue("mode", ((ShadersDefaultGradientMode)this.fill.atx()).ZZ());
      if ((Boolean)this.image.atx()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("image", b);
      if ((Boolean)this.syncColor.atx()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("syncColor", b);
      if ((Boolean)this.dots.atx()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("dots", b);
      managedShaderEffect.setUniformValue("dotRadius", (Integer)this.dotRadius.atx());
      managedShaderEffect.setUniformValue("imageTexture", 1);
      managedShaderEffect.setUniformValue(
         "color2",
         ((Color)this.fillColor.atx()).getRed() / 255.0F,
         ((Color)this.fillColor.atx()).getGreen() / 255.0F,
         ((Color)this.fillColor.atx()).getBlue() / 255.0F,
         ((Color)this.fillColor.atx()).getAlpha() / 255.0F
      );
      managedShaderEffect.setUniformValue("factor", (Float)this.factor.atx());
      if ((Boolean)this.mix.atx()) {
         b = 1;
      } else {
         b = 0;
      }

      managedShaderEffect.setUniformValue("mixColor", b);
      managedShaderEffect.setUniformValue("mixFactor", (Float)this.mixFactor.atx());
      managedShaderEffect.setUniformValue("saturation", (Float)this.saturation.atx());
      managedShaderEffect.setUniformValue("brightness", (Float)this.brightness.atx());
      managedShaderEffect.render(f);
      this.Z = (float)this.stopwatchImpl.afs() / 5.0F * (Float)this.speed.atx();
   }

   @EventListener
   public void onRenderLabelIfPresent(RenderLabelIfPresentEvent renderLabelIfPresentEvent) {
      if (renderLabelIfPresentEvent.getEntity() instanceof PlayerEntity && ab) {
         renderLabelIfPresentEvent.aim();
      }
   }

   @EventListener
   public void onRenderEvent3(RenderEvent3 renderEvent3) {
      if (client.player != null && !(Boolean)this.texture.atx() && (Boolean)this.crystals.atx() && !Chams.s && !ab) {
         EndCrystalEntity endCrystalEntity2 = renderEvent3.endCrystalEntity;
         if (client.player.squaredDistanceTo((Entity)endCrystalEntity2) <= ((NumberSetting)this.renderDistance).abq()) {
            renderEvent3.aim();
         }
      }
   }

   @EventListener
   public void onRenderPre(RenderPreEvent renderPreEvent) {
      if (client.player != null && !(Boolean)this.texture.atx() && this.agY(renderPreEvent.living) && !Chams.s && !ab) {
         LivingEntity living2 = renderPreEvent.living;
         if (!(client.player.squaredDistanceTo((Entity)living2) > ((NumberSetting)this.renderDistance).abq())) {
            renderPreEvent.aim();
            return;
         }
      }
   }

   @EventListener
   public void onRenderEvent7(RenderEvent7 renderEvent7) {
      if (client.player != null && !(Boolean)this.texture.atx() && (Boolean)this.items.atx() && !Chams.s && !ab) {
         ClientPlayerEntity temp = client.player;
         ItemEntity itemEntity = renderEvent7.getItemEntity();
         if (temp.squaredDistanceTo((Entity)itemEntity) <= ((NumberSetting)this.renderDistance).abq()) {
            renderEvent7.aim();
         }
      }
   }

   @EventListener
   public void onRenderFirstPersonItem_2(RenderFirstPersonItem_2Event renderFirstPersonItem2Event) {
      if (client.player != null && !(Boolean)this.texture.atx() && (Boolean)this.hands.atx() && !Chams.s && !ab) {
         renderFirstPersonItem2Event.aim();
      }
   }

   @EventListener
   public void onHasOutline(HasOutlineEvent hasOutlineEvent) {
      if (client.player != null && this.agY(hasOutlineEvent.getEntity())) {
         ClientPlayerEntity temp = client.player;
         Entity entity2 = hasOutlineEvent.getEntity();
         if (temp.squaredDistanceTo(entity2) > ((NumberSetting)this.renderDistance).abq()) {
            return;
         }

         hasOutlineEvent.aim();
      }
   }

   private void agV() {
      try {
         net.minecraft.client.texture.NativeImage image = null;
         String[] strs = new String[]{"png", "jpg"};

         for (String str : strs) {
            File file = Bootstrap.clientPaths.getPath().resolve("shader." + str).toFile();
            if (file.exists()) {
               try (java.io.InputStream inputStream = new FileInputStream(file)) {
                  image = net.minecraft.client.texture.NativeImage.read(inputStream);
               }
               break;
            }

            Optional optional = client.getResourceManager().getResource(Identifier.of("hachimiclient", "shaders/shader." + str));
            if (!optional.isEmpty() && ((Resource)optional.get()).getInputStream() != null) {
               try (java.io.InputStream inputStream = ((Resource)optional.get()).getInputStream()) {
                  image = net.minecraft.client.texture.NativeImage.read(inputStream);
               }
               break;
            }
         }

         if (image == null) {
            return;
         }

         net.minecraft.client.texture.NativeImageBackedTexture texture =
            new net.minecraft.client.texture.NativeImageBackedTexture(() -> "hachimi shader image", image);
         client.getTextureManager().registerTexture(Identifier.of("hachimiclient", "dynamic/shader_image"), texture);
         this.shaderImage = texture;
      } catch (IOException iOException) {
         iOException.printStackTrace();
      }
   }

   /** 1.21.11: 把用户图片作为 sampler 交给后效应，取代旧的 glActiveTexture/glBindTexture。 */
   private void agU(ManagedShaderEffect managedShaderEffect) {
      if (this.shaderImage == null) {
         return;
      }

      try {
         managedShaderEffect.setSamplerTexture("user", this.shaderImage.getGlTextureView());
      } catch (IllegalStateException ignored) {
      }
   }

   public Color agW(Entity entity2) {
      if (entity2 instanceof PlayerEntity) {
         if (entity2 == client.player) {
            return (Color)this.selfColor.atx();
         } else {
            return (Boolean)this.friendsColor.atx() && Managers.friendManager.awt(((PlayerEntity)entity2).getName())
               ? ColorHelper.wx().wB()
               : (Color)this.playersColor.atx();
         }
      } else if (BoxEntityUtils.kF(entity2)) {
         return (Color)this.monstersColor.atx();
      } else if (BoxEntityUtils.kH(entity2) || BoxEntityUtils.kI(entity2)) {
         return (Color)this.animalsColor.atx();
      } else if (entity2 instanceof EndCrystalEntity) {
         return (Color)this.crystalsColor.atx();
      } else if (entity2 instanceof ItemEntity) {
         return (Color)this.itemsColor.atx();
      } else {
         return !(entity2 instanceof ExperienceBottleEntity) && !(entity2 instanceof EnderPearlEntity) ? null : (Color)this.projectilesColor.atx();
      }
   }

   public Color agX(BlockEntity blockEntity) {
      if (blockEntity instanceof ChestBlockEntity) {
         return (Color)this.chestsColor.atx();
      } else if (blockEntity instanceof EnderChestBlockEntity) {
         return (Color)this.enderChestsColor.atx();
      } else {
         return blockEntity instanceof ShulkerBoxBlockEntity ? (Color)this.shulkersColor.atx() : null;
      }
   }

   public boolean agY(Entity entity2) {
      if (entity2 instanceof PlayerEntity && (Boolean)this.players.atx()) {
         boolean flag;
         if ((!(Boolean)this.self.atx() || client.options.getPerspective().isFirstPerson() && !Freecam.gP().awg()) && entity2 == client.player) {
            flag = false;
         } else {
            flag = true;
         }

         return flag;
      } else {
         boolean flag2;
         if ((!BoxEntityUtils.kF(entity2) || !(Boolean)this.monsters.atx())
            && (!BoxEntityUtils.kI(entity2) || !(Boolean)this.animals.atx())
            && (!(entity2 instanceof EndCrystalEntity) || !(Boolean)this.crystals.atx())
            && (!(entity2 instanceof ItemEntity) || !(Boolean)this.items.atx())
            && (!(entity2 instanceof ExperienceBottleEntity) || !(Boolean)this.projectiles.atx())
            && (!(entity2 instanceof EnderPearlEntity) || !(Boolean)this.projectiles.atx())) {
            flag2 = false;
         } else {
            flag2 = true;
         }

         return flag2;
      }
   }

   private boolean agZ(BlockEntity blockEntity) {
      boolean flag;
      if ((!(blockEntity instanceof ChestBlockEntity) || !(Boolean)this.chests.atx())
         && (!(blockEntity instanceof EnderChestBlockEntity) || !(Boolean)this.enderChests.atx())
         && (!(blockEntity instanceof ShulkerBoxBlockEntity) || !(Boolean)this.shulkers.atx())) {
         flag = false;
      } else {
         flag = true;
      }

      return flag;
   }

   static void aha(RenderWorld_2Event renderWorld2Event) {
      ab = true;
      ((GameRendererMixin)client.gameRenderer).hookRenderHand(renderWorld2Event.acs(), false, renderWorld2Event.getMatrices().peek().getPositionMatrix());
      ab = false;
   }

   void ahb(ManagedShaderEffect managedShaderEffect, RenderWorld_2Event renderWorld2Event) {
      this.agU(managedShaderEffect);
      managedShaderEffect.setUniformValue("sobel", 0);
      managedShaderEffect.setUniformValue(
         "color",
         ((Color)this.handsColor.atx()).getRed() / 255.0F,
         ((Color)this.handsColor.atx()).getGreen() / 255.0F,
         ((Color)this.handsColor.atx()).getBlue() / 255.0F,
         (Float)this.opacity.atx()
      );
      this.agO(renderWorld2Event.acs(), managedShaderEffect);
   }

   void ahc(RenderWorldEvent renderWorldEvent) {
      net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getCameraPos();
      float tickDelta = renderWorldEvent.ll();
      // 事件阶段 GameRendererMixin2 把 modelview 设成 R * M^-1（M = tilt/bob 栈），
      // 与投影 P*M 相抵，所以这里必须用事件矩阵，实体才会和世界对齐。
      MatrixStack matrices = renderWorldEvent.getMatrices();
      net.minecraft.client.render.state.CameraRenderState cameraState = new net.minecraft.client.render.state.CameraRenderState();
      cameraState.initialized = true;
      cameraState.pos = cameraPos;
      cameraState.blockPos = camera.getBlockPos();
      cameraState.entityPos = camera.getFocusedEntity() != null ? camera.getFocusedEntity().getEntityPos() : cameraPos;
      cameraState.orientation = new org.joml.Quaternionf(camera.getRotation());

      ab = true;
      net.minecraft.client.render.command.OrderedRenderCommandQueueImpl queue =
         pw.hachimi.client.render.EntityMaskRenderer.begin();
      EntityRenderManager entityRenderManager = client.getEntityRenderDispatcher();
      boolean flat = !(Boolean)this.texture.atx();

      for (Entity entity2 : client.world.getEntities()) {
         if (NoRender.ff().awg() && NoRender.ff().fM(entity2)) {
            continue;
         }
         if (!RenderUtils.acy(entity2.getBoundingBox()) || !this.agY(entity2)) {
            continue;
         }

         Vec3d vec = Freecam.gP().awg() ? Freecam.gP().getVec() : client.player.getEntityPos();
         if (vec.squaredDistanceTo(entity2.getEntityPos()) > ((NumberSetting)this.renderDistance).abq()) {
            continue;
         }

         matrices.push();
         try {
            net.minecraft.client.render.entity.state.EntityRenderState state =
               entityRenderManager.getAndUpdateRenderState(entity2, tickDelta);
            if (flat) {
               Color color = this.agW(entity2);
               if (color != null) {
                  state.outlineColor = 0xFF000000 | color.getRGB();
               }
            }

            double d = MathHelper.lerp(tickDelta, entity2.lastRenderX, entity2.getX()) - cameraPos.x;
            double d2 = MathHelper.lerp(tickDelta, entity2.lastRenderY, entity2.getY()) - cameraPos.y;
            double d3 = MathHelper.lerp(tickDelta, entity2.lastRenderZ, entity2.getZ()) - cameraPos.z;
            entityRenderManager.render(state, cameraState, d, d2, d3, matrices, queue);
         } catch (Throwable throwable) {
            // 单个实体渲染失败不能拖垮整帧
         } finally {
            matrices.pop();
         }
      }

      if ((Boolean)this.enderChests.atx() || (Boolean)this.chests.atx() || (Boolean)this.shulkers.atx()) {
         BlockEntityRenderManager blockEntityRenderManager = client.getBlockEntityRenderDispatcher();

         for (Object object : CollisionUtils.aiv()) {
            BlockEntity blockEntity = (BlockEntity)object;
            if (!this.agZ(blockEntity)) {
               continue;
            }

            matrices.push();
            try {
               net.minecraft.client.render.block.entity.state.BlockEntityRenderState state =
                  blockEntityRenderManager.getRenderState(blockEntity, tickDelta, null);
               if (state != null) {
                  Vec3d center = Vec3d.ofCenter(blockEntity.getPos());
                  matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
                  blockEntityRenderManager.render(state, matrices, queue, cameraState);
               }
            } catch (Throwable throwable) {
            } finally {
               matrices.pop();
            }
         }
      }

      pw.hachimi.client.render.EntityMaskRenderer.end(CompatRender.getTarget());
      ab = false;
   }

   void ahd(ManagedShaderEffect managedShaderEffect, RenderWorldEvent renderWorldEvent) {
      managedShaderEffect.setUniformValue("sobel", 1);
      managedShaderEffect.setUniformValue("color", 1.0F, 1.0F, 1.0F, (Float)this.opacity.atx());
      this.agO(renderWorldEvent.ll(), managedShaderEffect);
   }

   private Boolean isShulkersColorVisible() {
      return (Boolean)this.shulkers.atx();
   }

   private Boolean isEnderChestsColorVisible() {
      return (Boolean)this.enderChests.atx();
   }

   private Boolean isChestsColorVisible() {
      return (Boolean)this.chests.atx();
   }

   private Boolean isProjectilesColorVisible() {
      return (Boolean)this.projectiles.atx();
   }

   private Boolean isCrystalsColorVisible() {
      return (Boolean)this.crystals.atx();
   }

   private Boolean isItemsColorVisible() {
      return (Boolean)this.items.atx();
   }

   private Boolean isAnimalsColorVisible() {
      return (Boolean)this.animals.atx();
   }

   private Boolean isMonstersColorVisible() {
      return (Boolean)this.monsters.atx();
   }

   private Boolean isPlayersColorVisible() {
      return (Boolean)this.players.atx();
   }

   private Boolean isSelfColorVisible() {
      return (Boolean)this.self.atx();
   }

   private Boolean isHandsColorVisible() {
      return (Boolean)this.hands.atx();
   }

   private Boolean isMixFactorVisible() {
      boolean flag;
      if ((Boolean)this.mix.atx() && (Boolean)this.image.atx()) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isMixVisible() {
      return (Boolean)this.image.atx();
   }

   private Boolean isBrightnessVisible() {
      boolean flag;
      if (this.fill.atx() == ShadersDefaultGradientMode.RAINBOW) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isSaturationVisible() {
      boolean flag;
      if (this.fill.atx() == ShadersDefaultGradientMode.RAINBOW) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isFactorVisible() {
      boolean flag;
      if (this.fill.atx() != ShadersDefaultGradientMode.GRADIENT && this.fill.atx() != ShadersDefaultGradientMode.RAINBOW) {
         flag = false;
      } else {
         flag = true;
      }

      return flag;
   }

   private Boolean isFillColorVisible() {
      boolean flag;
      if (this.fill.atx() == ShadersDefaultGradientMode.GRADIENT) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isDotRadiusVisible() {
      return (Boolean)this.dots.atx();
   }

   private Boolean isGlowFactorVisible() {
      boolean flag;
      if ((Boolean)this.glow.atx() && this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isInnerVisible() {
      boolean flag;
      if ((Boolean)this.glow.atx() && this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isGlowVisible() {
      boolean flag;
      if (this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isStepsVisible() {
      boolean flag;
      if ((Boolean)this.fastOutline.atx() && this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isQualityVisible() {
      boolean flag;
      if ((Boolean)this.fastOutline.atx() && this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isFastOutlineVisible() {
      boolean flag;
      if (this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isWidthVisible() {
      boolean flag;
      if (this.outline.atx() == ShadersCustomBloomMode.Custom) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isSyncColorVisible() {
      boolean flag;
      if (this.outline.atx() != ShadersCustomBloomMode.Off) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private static Color b_1() {
      return new Color(1.0F, 1.0F, 1.0F);
   }

   private static ColorSetting createColorSetting(String str, String str2, Color color3, Supplier supplier) {
      return new ColorSetting(str, str2, color3, false, supplier);
   }

   private static ColorSetting createColorSetting(String str, String str2, Color color3, boolean flag, Supplier supplier) {
      return new ColorSetting(str, str2, color3, flag, false, supplier);
   }
}
