package pw.hachimi.client.utils.player;


import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.component.type.ToolComponent;

/**
 * 1.21.11 里 SwordItem / PickaxeItem / ArmorItem / ElytraItem / ToolItem / ChorusFruitItem
 * 这些类被删除，物品改用数据组件表达。
 * 这个类把旧代码里的 instanceof 判断统一收敛到这里，保证行为一致。
 */
public final class ItemCompat {
   private ItemCompat() {
   }

   public static boolean isSword(Item item) {
      return item == null ? false : item.getDefaultStack().isIn(ItemTags.SWORDS);
   }

   public static boolean isPickaxe(Item item) {
      return item == null ? false : item.getDefaultStack().isIn(ItemTags.PICKAXES);
   }

   public static boolean isElytra(Item item) {
      return item == Items.ELYTRA;
   }

   public static boolean isChorusFruit(Item item) {
      return item == Items.CHORUS_FRUIT;
   }

   public static boolean isTool(Item item) {
      return item != null && item.getComponents().contains(DataComponentTypes.TOOL);
   }

   public static EquippableComponent equippable(Item item) {
      return item == null ? null : item.getComponents().get(DataComponentTypes.EQUIPPABLE);
   }

   /** 旧 ArmorItem instanceof 判断的等价物：有装备组件且槽位是头盔/胸甲/护腿/靴子。 */
   public static boolean isArmor(Item item) {
      EquippableComponent component = equippable(item);
      return component != null && component.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
   }

   public static boolean isArmor(ItemStack stack) {
      if (stack == null) {
         return false;
      }
      EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
      return component != null && component.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
   }

   /** 旧 ArmorItem#getSlotType()。 */
   public static EquipmentSlot armorSlot(Item item) {
      EquippableComponent component = equippable(item);
      return component == null ? null : component.slot();
   }

   /** 旧 ArmorItem#getMaterial()，返回装备材质 id（EquipmentAssets.NETHERITE 等）。 */
   public static Optional<RegistryKey<EquipmentAsset>> armorMaterial(Item item) {
      EquippableComponent component = equippable(item);
      return component == null ? Optional.empty() : component.assetId();
   }

   /** 旧 ElytraItem.isUsable(stack)：滑翔翼且未损坏到无法使用。 */
   public static boolean isUsableElytra(ItemStack stack) {
      return stack != null
         && stack.isOf(Items.ELYTRA)
         && stack.getMaxDamage() - stack.getDamage() > 1;
   }

   /**
    * 旧 ArmorMaterial#getProtection(type) 的等价物。
    * 1.21.11 里护甲值改为属性修饰符，直接按 ARMOR 属性求和。
    */
   public static double armorValue(ItemStack stack, EquipmentSlot slot) {
      if (stack == null || stack.isEmpty()) {
         return 0.0;
      }
      net.minecraft.component.type.AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0;
      }
      return modifiers.applyOperations(
         net.minecraft.entity.attribute.EntityAttributes.ARMOR,
         0.0,
         slot == null ? EquipmentSlot.CHEST : slot);
   }
}
