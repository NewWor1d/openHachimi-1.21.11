package pw.hachimi.client.utils;

import net.minecraft.util.PlayerInput;

/**
 * 1.21.11 的 {@link PlayerInput} 是不可变 record，旧的
 * {@code input.pressingForward = true} / {@code input.jumping = true} 写法
 * 一律改成"复制一份、只覆盖某一位"。
 */
public final class InputCompat {
   private InputCompat() {
   }

   public static PlayerInput withForward(PlayerInput in, boolean value) {
      return new PlayerInput(value, in.backward(), in.left(), in.right(), in.jump(), in.sneak(), in.sprint());
   }

   public static PlayerInput withBackward(PlayerInput in, boolean value) {
      return new PlayerInput(in.forward(), value, in.left(), in.right(), in.jump(), in.sneak(), in.sprint());
   }

   public static PlayerInput withLeft(PlayerInput in, boolean value) {
      return new PlayerInput(in.forward(), in.backward(), value, in.right(), in.jump(), in.sneak(), in.sprint());
   }

   public static PlayerInput withRight(PlayerInput in, boolean value) {
      return new PlayerInput(in.forward(), in.backward(), in.left(), value, in.jump(), in.sneak(), in.sprint());
   }

   public static PlayerInput withJump(PlayerInput in, boolean value) {
      return new PlayerInput(in.forward(), in.backward(), in.left(), in.right(), value, in.sneak(), in.sprint());
   }

   public static PlayerInput withSneak(PlayerInput in, boolean value) {
      return new PlayerInput(in.forward(), in.backward(), in.left(), in.right(), in.jump(), value, in.sprint());
   }

   public static PlayerInput withSprint(PlayerInput in, boolean value) {
      return new PlayerInput(in.forward(), in.backward(), in.left(), in.right(), in.jump(), in.sneak(), value);
   }
}
