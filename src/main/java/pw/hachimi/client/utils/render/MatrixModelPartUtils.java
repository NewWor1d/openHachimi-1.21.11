package pw.hachimi.client.utils.render;

import com.mojang.datafixers.util.Pair;
import java.awt.Color;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.model.ModelPart.Quad;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.SheepWoolFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.LlamaEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.render.entity.model.SheepWoolEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import pw.hachimi.client.Mc;
import pw.hachimi.client.model.render.ShapeMode;
import pw.hachimi.client.module.HitResultWorldHelper;
import pw.hachimi.client.module.render.CrystalModel;
import pw.hachimi.client.render.BoxRenderer;
import pw.hachimi.client.utils.player.PlayerMathHelperUtils;

public class MatrixModelPartUtils {
   private static final MatrixStack a_ = new MatrixStack();
   private static final Vector4f b = new Vector4f();
   private static final Vector4f c = new Vector4f();
   private static final Vector4f d = new Vector4f();
   private static final Vector4f e = new Vector4f();
   private static double f;
   private static double g;
   private static double h;
   private static Color color;
   private static Color color2;
   private static ShapeMode shapeMode;
   private static final long a = 0L;

   private MatrixModelPartUtils() {
   }

   public static void hX(Render3DContext render3DContext, Entity entity2, Color color3, Color color4, ShapeMode shapeMode2) {
      hY(render3DContext, render3DContext.f, entity2, 1.0, color3, color4, shapeMode2, 0.0F, false, false);
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static void hY(
      Render3DContext render3DContext, float f2, Entity entity2, double d2, Color color3, Color color4, ShapeMode shapeMode2, float f3, boolean flag, boolean flag2
   ) {
      color = color3;
      color2 = color4;
      shapeMode = shapeMode2;
      f = MathHelper.lerp(f2, entity2.lastRenderX, entity2.getX());
      g = MathHelper.lerp(f2, entity2.lastRenderY, entity2.getY());
      h = MathHelper.lerp(f2, entity2.lastRenderZ, entity2.getZ());
      a_.push();
      float f4;
      float f5 = f4 = (float)d2;
      float f6 = (float)d2;
      a_.scale(f6, f5, f4);
      EntityRenderer entityRenderer = Mc.client.getEntityRenderDispatcher().getRenderer(entity2);
      if (entityRenderer instanceof LivingEntityRenderer) {
         LivingEntityRenderer livingRenderer = (LivingEntityRenderer)entityRenderer;
         EntityModel entityModel = livingRenderer.getModel();
         // 1.21.11: 姿势计算全部搬进 RenderState，直接让渲染器更新一份状态再喂给模型。
         LivingEntityRenderState state = (LivingEntityRenderState)livingRenderer.getAndUpdateRenderState((LivingEntity)entity2, f2);
         if (flag2) {
            a_.translate(0.0, -0.125, 0.0);
         }

         entityModel.setAngles(state);
         livingRenderer.setupTransforms(state, a_, state.bodyYaw, state.pitch);
         a_.scale(-1.0F, -1.0F, 1.0F);
         livingRenderer.scale(state, a_);
         a_.translate(0.0, -1.501F, 0.0);
         if (entityModel instanceof BipedEntityModel) {
            BipedEntityModel biped = (BipedEntityModel)entityModel;
            ia(render3DContext.boxRenderer, biped.head);
            ia(render3DContext.boxRenderer, biped.body);
            ia(render3DContext.boxRenderer, biped.leftArm);
            ia(render3DContext.boxRenderer, biped.rightArm);
            ia(render3DContext.boxRenderer, biped.leftLeg);
            ia(render3DContext.boxRenderer, biped.rightLeg);
         } else if (entityModel instanceof net.minecraft.client.model.Model) {
            for (net.minecraft.client.model.ModelPart part : ((net.minecraft.client.model.Model<?>)entityModel).getParts()) {
               ih(render3DContext, part);
            }
         }

         hZ(render3DContext.boxRenderer, (LivingEntityRenderer)entityRenderer, (LivingEntity)entity2, f2,
            state.bodyYaw, state.relativeHeadYaw, state.pitch,
            state.limbSwingAnimationProgress, state.limbSwingAmplitude);
      }

      if (entityRenderer instanceof EndCrystalEntityRenderer) {
         CrystalModel crystalModel = CrystalModel.kh();
         boolean flag6 = crystalModel.awg();
         a_.push();
         float f32;
         if (flag6 && !crystalModel.kn()) {
            f32 = -1.2F;
         } else {
            f32 = EndCrystalEntityRenderer.getYOffset(((EndCrystalEntity)entity2).endCrystalAge + f2);
         }

         if (flag6) {
            f32 += crystalModel.kq();
         }

         float f33;
         if (flag6) {
            f33 = (((EndCrystalEntity)entity2).endCrystalAge + f2) * crystalModel.ko() * 3.0F;
         } else {
            f33 = (((EndCrystalEntity)entity2).endCrystalAge + f2) * 3.0F;
         }

         a_.push();
         if (flag6) {
            MatrixStack matrices3 = a_;
            float f34;
            float f35 = f34 = 2.0F * crystalModel.kp();
            float f36 = 2.0F * crystalModel.kp();
            matrices3.scale(f36, f35, f34);
         } else {
            a_.scale(2.0F, 2.0F, 2.0F);
         }

         a_.translate(0.0, -0.5, 0.0);
         if (((EndCrystalEntity)entity2).shouldShowBottom()) {
            ia(render3DContext.boxRenderer, ((EndCrystalEntityRenderer)entityRenderer).model.base);
         }

         MatrixStack matrices4 = a_;
         Quaternionf quaternionf = RotationAxis.POSITIVE_Y.rotationDegrees(f33);
         matrices4.multiply(quaternionf);
         double d7 = 1.5F + f32 / 2.0F;
         a_.translate(0.0, d7, 0.0);
         matrices4 = a_;
         Quaternionf quaternionf2 = new Quaternionf()
            .setAngleAxis((float) (Math.PI / 3), 0.70710677F, 0.0F, 0.70710677F);
         matrices4.multiply(quaternionf2);
         ia(render3DContext.boxRenderer, ((EndCrystalEntityRenderer)entityRenderer).model.outerGlass);
         a_.scale(0.875F, 0.875F, 0.875F);
         matrices4 = a_;
         Quaternionf quaternionf3 = new Quaternionf()
            .setAngleAxis((float) (Math.PI / 3), 0.70710677F, 0.0F, 0.70710677F);
         matrices4.multiply(quaternionf3);
         matrices4 = a_;
         Quaternionf quaternionf4 = RotationAxis.POSITIVE_Y.rotationDegrees(f33);
         matrices4.multiply(quaternionf4);
         ia(render3DContext.boxRenderer, ((EndCrystalEntityRenderer)entityRenderer).model.outerGlass);
         a_.scale(0.875F, 0.875F, 0.875F);
         matrices4 = a_;
         Quaternionf quaternionf5 = new Quaternionf()
            .setAngleAxis((float) (Math.PI / 3), 0.70710677F, 0.0F, 0.70710677F);
         matrices4.multiply(quaternionf5);
         matrices4 = a_;
         Quaternionf quaternionf6 = RotationAxis.POSITIVE_Y.rotationDegrees(f33);
         matrices4.multiply(quaternionf6);
         ia(render3DContext.boxRenderer, ((EndCrystalEntityRenderer)entityRenderer).model.innerGlass);
         a_.pop();
         a_.pop();
      } else if (entityRenderer instanceof BoatEntityRenderer) {
         a_.push();
         a_.translate(0.0, 0.375, 0.0);
         MatrixStack matrices5 = a_;
         RotationAxis rotationAxis = RotationAxis.POSITIVE_Y;
         float f37 = 0.0F - MathHelper.lerp(f2, entity2.lastYaw, entity2.getYaw());
         Quaternionf quaternionf7 = rotationAxis.rotationDegrees(f37);
         matrices5.multiply(quaternionf7);
         float f38 = ((BoatEntity)entity2).getDamageWobbleTicks() - f2;
         float f39 = ((BoatEntity)entity2).getDamageWobbleStrength() - f2;
         if (f39 < 0.0F) {
            f39 = 0.0F;
         }

         if (f38 > 0.0F) {
            matrices5 = a_;
            rotationAxis = RotationAxis.POSITIVE_X;
            float f40 = MathHelper.sin(f38) * f38 * f39 / 10.0F * ((BoatEntity)entity2).getDamageWobbleSide();
            Quaternionf quaternionf8 = rotationAxis.rotationDegrees(f40);
            matrices5.multiply(quaternionf8);
         }

         // 1.21.11: 船的模型与材质合并进 BoatEntityRenderer#model，分型数据不再暴露 Entity.Type。
         EntityModel boatModel = ((BoatEntityRenderer)entityRenderer).model;
         a_.scale(-1.0F, -1.0F, 1.0F);
         a_.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.0F));
         for (ModelPart part : ((net.minecraft.client.model.Model<?>)boatModel).getParts()) {
            ie(render3DContext, part);
         }

         a_.pop();
      } else if (entityRenderer instanceof ItemEntityRenderer) {
         double d8 = (entity2.getX() - entity2.lastX) * f2;
         double d9 = (entity2.getY() - entity2.lastY) * f2;
         double d10 = (entity2.getZ() - entity2.lastZ) * f2;
         Box box = entity2.getBoundingBox();
         render3DContext.boxRenderer
            .YY(
               d8 + box.minX,
               d9 + box.minY,
               d10 + box.minZ,
               d8 + box.maxX,
               d9 + box.maxY,
               d10 + box.maxZ,
               color3,
               color4,
               shapeMode2,
               0
            );
      }

      a_.pop();
   }

   private static void hZ(
      BoxRenderer boxRenderer2, LivingEntityRenderer livingEntityRenderer, LivingEntity living, float f2, float f3, float f4, float f5, float f6, float f7
   ) {
      Iterator iterator2 = livingEntityRenderer.features.iterator();

      while (iterator2.hasNext()) {
         FeatureRenderer featureRenderer = (FeatureRenderer)iterator2.next();
         if (featureRenderer instanceof SheepWoolFeatureRenderer && living instanceof SheepEntity && !((SheepEntity)living).isSheared() && !((SheepEntity)living).isInvisible()
            )
          {
            // 1.21.11: 羊毛模型改为 EntityModel<SheepEntityRenderState>，姿势走 RenderState。
            EntityModel woolModel = ((SheepWoolFeatureRenderer)featureRenderer).woolModel;
            net.minecraft.client.render.entity.state.LivingEntityRenderState woolState =
               (net.minecraft.client.render.entity.state.LivingEntityRenderState)livingEntityRenderer.getAndUpdateRenderState(living, f7);
            woolModel.setAngles(woolState);
            for (ModelPart part : ((net.minecraft.client.model.Model<?>)woolModel).getParts()) {
               ic(boxRenderer2, part);
            }
         }
      }
   }

   private static void ia(BoxRenderer boxRenderer2, ModelPart modelPart) {
      if (modelPart.visible && (!modelPart.cuboids.isEmpty() || !modelPart.children.isEmpty())) {
         a_.push();
         MatrixStack matrices = a_;
         modelPart.applyTransform(matrices);
         Iterator iterator2 = modelPart.cuboids.iterator();

         while (iterator2.hasNext()) {
            Cuboid cuboid = (Cuboid)iterator2.next();
            ib(boxRenderer2, cuboid, f, g, h);
         }

         iterator2 = modelPart.children.values().iterator();

         while (iterator2.hasNext()) {
            ModelPart modelPart2 = (ModelPart)iterator2.next();
            ia(boxRenderer2, modelPart2);
         }

         a_.pop();
      }
   }

   private static void ib(BoxRenderer boxRenderer2, Cuboid cuboid, double d2, double d3, double d4) {
      Matrix4f matrix4f = a_.peek().getPositionMatrix();

      for (Quad quad : cuboid.sides) {
         b.set(quad.vertices[0].x / 16.0F, quad.vertices[0].y / 16.0F, quad.vertices[0].z / 16.0F, 1.0F);
         b.mul(matrix4f);
         c.set(quad.vertices[1].x / 16.0F, quad.vertices[1].y / 16.0F, quad.vertices[1].z / 16.0F, 1.0F);
         c.mul(matrix4f);
         d.set(quad.vertices[2].x / 16.0F, quad.vertices[2].y / 16.0F, quad.vertices[2].z / 16.0F, 1.0F);
         d.mul(matrix4f);
         e.set(quad.vertices[3].x / 16.0F, quad.vertices[3].y / 16.0F, quad.vertices[3].z / 16.0F, 1.0F);
         e.mul(matrix4f);
         if (shapeMode.KL()) {
            boxRenderer2.vec3dGameRendererHelper2
               .pD(
                  boxRenderer2.vec3dGameRendererHelper2.py(d2 + b.x, d3 + b.y, d4 + b.z).pA(color).pB(),
                  boxRenderer2.vec3dGameRendererHelper2.py(d2 + c.x, d3 + c.y, d4 + c.z).pA(color).pB(),
                  boxRenderer2.vec3dGameRendererHelper2.py(d2 + d.x, d3 + d.y, d4 + d.z).pA(color).pB(),
                  boxRenderer2.vec3dGameRendererHelper2.py(d2 + e.x, d3 + e.y, d4 + e.z).pA(color).pB()
               );
         }

         if (shapeMode.KK()) {
            boxRenderer2.YJ(d2 + b.x, d3 + b.y, d4 + b.z, d2 + c.x, d3 + c.y, d4 + c.z, color2);
            boxRenderer2.YJ(d2 + c.x, d3 + c.y, d4 + c.z, d2 + d.x, d3 + d.y, d4 + d.z, color2);
            boxRenderer2.YJ(d2 + d.x, d3 + d.y, d4 + d.z, d2 + e.x, d3 + e.y, d4 + e.z, color2);
            boxRenderer2.YJ(d2 + b.x, d3 + b.y, d4 + b.z, d2 + b.x, d3 + b.y, d4 + b.z, color2);
         }
      }
   }

   static void ic(BoxRenderer boxRenderer2, ModelPart modelPart) {
      ia(boxRenderer2, modelPart);
   }

   static void id(BoxRenderer boxRenderer2, ModelPart modelPart) {
      ia(boxRenderer2, modelPart);
   }

   static void ie(Render3DContext render3DContext, ModelPart modelPart) {
      ia(render3DContext.boxRenderer, modelPart);
   }

   static void if_(Render3DContext render3DContext, Object obj) {
      ia(render3DContext.boxRenderer, (ModelPart)obj);
   }

   static void ig(Render3DContext render3DContext, Object obj) {
      ia(render3DContext.boxRenderer, (ModelPart)obj);
   }

   static void ih(Render3DContext render3DContext, Object obj) {
      ia(render3DContext.boxRenderer, (ModelPart)obj);
   }

   static void ii(Render3DContext render3DContext, Object obj) {
      ia(render3DContext.boxRenderer, (ModelPart)obj);
   }

   static void ij(Render3DContext render3DContext, Object obj) {
      ia(render3DContext.boxRenderer, (ModelPart)obj);
   }
}
