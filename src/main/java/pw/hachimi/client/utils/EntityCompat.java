package pw.hachimi.client.utils;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;

/** 1.21.11 里 {@code net.minecraft.entity.Tameable} 接口被删除，统一到 TameableEntity。 */
public final class EntityCompat {
   private EntityCompat() {
   }

   /** 旧 Tameable#getOwnerUuid()。未驯服返回 null。 */
   public static UUID getOwnerUuid(Entity entity) {
      if (entity instanceof TameableEntity tameable) {
         LazyEntityReference<LivingEntity> reference = tameable.getOwnerReference();
         return reference == null ? null : reference.getUuid();
      }
      return null;
   }

   public static boolean isTameable(Entity entity) {
      return entity instanceof TameableEntity;
   }

   /** 旧 LivingEntity#getArmorItems()：按 脚→腿→胸→头 顺序返回已装备的护甲。 */
   public static java.util.List<net.minecraft.item.ItemStack> getArmorItems(LivingEntity entity) {
      java.util.List<net.minecraft.item.ItemStack> list = new java.util.ArrayList<>(4);
      if (entity != null) {
         list.add(entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET));
         list.add(entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS));
         list.add(entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST));
         list.add(entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD));
      }
      return list;
   }

   /** 旧 PlayerInventory.armor 的下标顺序：0=靴 1=腿 2=胸 3=头。 */
   public static net.minecraft.item.ItemStack getArmorStack(LivingEntity entity, int index) {
      if (entity == null) {
         return net.minecraft.item.ItemStack.EMPTY;
      }
      switch (index) {
         case 0:
            return entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);
         case 1:
            return entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
         case 2:
            return entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
         default:
            return entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
      }
   }

   /** 旧 LivingEntity#blockedByShield(DamageSource) 的近似实现：是否处于举盾格挡状态。 */
   public static boolean blockedByShield(LivingEntity entity) {
      return entity != null && entity.isBlocking();
   }
}
