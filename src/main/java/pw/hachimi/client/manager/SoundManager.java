package pw.hachimi.client.manager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import pw.hachimi.client.Mc;

public class SoundManager {
   public static final SoundEvent a = jf("gui_click");
   public static final SoundEvent b = jf("twitter");
   public static final SoundEvent c = jf("ios");
   public static final SoundEvent d = jf("discord");
   public static final SoundEvent e = jf("steam");
   public static final SoundEvent f = jf("add_entity");
   public static final SoundEvent g = jf("enable");
   public static final SoundEvent h = jf("disable");

   private static SoundEvent jf(String str) {
      // 1.21.11: 客户端初始化时 SOUND_EVENT 注册表已冻结，不能再用 Registry.register。
      // 播放只需要 sounds.json 里同 id 的条目，未注册的 SoundEvent 也可直接播放。
      return SoundEvent.of(Identifier.of("hachimiclient", str));
   }

   public void jg(SoundEvent soundEvent) {
      this.jh(soundEvent);
   }

   public void jh(SoundEvent soundEvent) {
      if (Mc.client.player != null) {
         MinecraftClient temp = Mc.client;
         final SoundEvent soundEvent2 = soundEvent;
         Runnable runnable = new Runnable() {
            @Override
            public void run() {
               SoundManager.ji(soundEvent2);
            }
         };
         temp.executeSync(runnable);
      }
   }

   static void ji(SoundEvent soundEvent) {
      Mc.client.player.playSound(soundEvent, 2.0F, 1.0F);
   }
}
