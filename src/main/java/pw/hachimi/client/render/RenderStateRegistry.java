package pw.hachimi.client.render;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;

/**
 * 1.21.11：实体/方块实体渲染改成 RenderState 管线，render 阶段拿不到原始 Entity/BlockEntity。
 * 这里在 updateRenderState 时把 state → 实体 的映射记下来，渲染钩子里反查。
 *
 * <p>使用 WeakHashMap（EntityRenderState 没有重写 equals/hashCode，等价于恒等比较），
 * 避免状态对象被长期持有。
 */
public final class RenderStateRegistry {
   private static final Map<Object, Entity> ENTITIES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<Object, BlockEntity> BLOCK_ENTITIES = Collections.synchronizedMap(new WeakHashMap<>());

   private RenderStateRegistry() {
   }

   public static void putEntity(Object state, Entity entity) {
      if (state != null) {
         ENTITIES.put(state, entity);
      }
   }

   public static Entity getEntity(Object state) {
      return state == null ? null : ENTITIES.get(state);
   }

   public static void putBlockEntity(Object state, BlockEntity blockEntity) {
      if (state != null) {
         BLOCK_ENTITIES.put(state, blockEntity);
      }
   }

   public static BlockEntity getBlockEntity(Object state) {
      return state == null ? null : BLOCK_ENTITIES.get(state);
   }
}
