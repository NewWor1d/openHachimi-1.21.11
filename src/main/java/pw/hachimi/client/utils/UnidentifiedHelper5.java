package pw.hachimi.client.utils;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class UnidentifiedHelper5 {
   public static final int[] a = new int[Mode.values().length];

   static {
      try {
         a[Mode.START_SPRINTING.ordinal()] = 1;
      } catch (NoSuchFieldError noSuchFieldError) {
      }

      try {
         a[Mode.STOP_SPRINTING.ordinal()] = 2;
      } catch (NoSuchFieldError noSuchFieldError) {
      }


   }
}
