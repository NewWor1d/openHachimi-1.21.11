package pw.hachimi.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pw.hachimi.client.Mc;
import pw.hachimi.client.event.render.GetCollisionShapeEvent;
import pw.hachimi.eventbus.EventBusImpl;

@Mixin(BlockCollisionSpliterator.class)
public class BlockCollisionSpliteratorMixin {
   // 1.21.11: 碰撞形状改为 ShapeContext#getCollisionShape(BlockState, CollisionView, BlockPos)
   @Redirect(
      method = "computeNext",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/ShapeContext;getCollisionShape(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/CollisionView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;"
      )
   )
   private VoxelShape hookGetCollisionShape(ShapeContext shapeContext, BlockState state, net.minecraft.world.CollisionView collisionView, BlockPos pos) {
      VoxelShape voxelShape = shapeContext.getCollisionShape(state, collisionView, pos);
      if (collisionView != Mc.client.world) {
         return voxelShape;
      }

      GetCollisionShapeEvent getCollisionShapeEvent = new GetCollisionShapeEvent(voxelShape, pos, state);
      EventBusImpl.INSTANCE.rC(getCollisionShapeEvent);
      return getCollisionShapeEvent.aik() ? getCollisionShapeEvent.getVoxelShape() : voxelShape;
   }
}
