package pw.hachimi.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;

/**
 * 1.21.11 删除了 {@code PickFromInventoryC2SPacket}（生存模式把背包物品换到手上的包）。
 * 等价操作改为走 {@code ClientPlayerInteractionManager.clickSlot(...)} 的 SWAP 动作，
 * 由客户端自己构造 ClickSlotC2SPacket，避免手拼 syncId/revision 出错。
 */
public final class PacketCompat {
   private PacketCompat() {
   }

   /** 等价于旧 PickFromInventoryC2SPacket(slot)：把背包 slot 与当前选中快捷栏交换。 */
   public static void pickFromInventory(int slot) {
      MinecraftClient client = MinecraftClient.getInstance();
      ClientPlayerEntity player = client.player;
      if (player == null || client.interactionManager == null) {
         return;
      }
      int hotbarSlot = player.getInventory().selectedSlot;
      client.interactionManager.clickSlot(player.currentScreenHandler.syncId, slot, hotbarSlot, SlotActionType.SWAP, player);
   }

   /**
    * 1.21.11: ClientCommandC2SPacket.Mode 里的 PRESS_SHIFT_KEY / RELEASE_SHIFT_KEY 被删除，
    * 潜行状态改由 PlayerInputC2SPacket 承载。这里保持当前输入状态、只覆盖 sneak 位。
    */
   public static net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket sneakPacket(boolean sneaking) {
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      net.minecraft.util.PlayerInput base = client.player == null
         ? net.minecraft.util.PlayerInput.DEFAULT
         : client.player.input.playerInput;
      net.minecraft.util.PlayerInput input = new net.minecraft.util.PlayerInput(
         base.forward(), base.backward(), base.left(), base.right(), base.jump(), sneaking, base.sprint());
      return new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(input);
   }

   /**
    * 1.21.11: ClickSlotC2SPacket 的 slot/button 缩成 short/byte，
    * 且 modifiedStacks/cursor 改成 {@code ItemStackHash}（需要 ComponentHasher）。
    * 这里集中把旧的"ItemStack + Int2ObjectMap&lt;ItemStack&gt;"写法转成新包。
    */
   public static net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket clickSlot(
      int syncId,
      int revision,
      int slot,
      int button,
      net.minecraft.screen.slot.SlotActionType actionType,
      net.minecraft.item.ItemStack cursor,
      it.unimi.dsi.fastutil.ints.Int2ObjectMap modifiedStacks
   ) {
      net.minecraft.client.network.ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
      net.minecraft.screen.sync.ComponentChangesHash.ComponentHasher hasher = handler == null ? component -> 0 : handler.getComponentHasher();
      it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraft.screen.sync.ItemStackHash> hashed = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
      if (modifiedStacks != null) {
         for (Object entryObject : modifiedStacks.int2ObjectEntrySet()) {
            it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry entry = (it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry)entryObject;
            hashed.put(
               entry.getIntKey(),
               net.minecraft.screen.sync.ItemStackHash.fromItemStack((net.minecraft.item.ItemStack)entry.getValue(), hasher)
            );
         }
      }

      return new net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket(
         syncId,
         revision,
         (short)slot,
         (byte)button,
         actionType,
         hashed,
         net.minecraft.screen.sync.ItemStackHash.fromItemStack(cursor == null ? net.minecraft.item.ItemStack.EMPTY : cursor, hasher)
      );
   }
}
