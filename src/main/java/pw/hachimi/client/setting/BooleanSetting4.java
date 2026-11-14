package pw.hachimi.client.setting;

import pw.hachimi.client.config.ConfigContainer;
import pw.hachimi.client.module.ClientModule;
import pw.hachimi.eventbus.EventBusImpl;

public class BooleanSetting4 extends BooleanSetting {
   public static boolean fk;

   public BooleanSetting4(String str, String str2, Boolean flag) {
      super(str, str2, flag);
   }

   @Override
   public void Kl(Boolean flag) {
      if (System.getProperty("hachimi.testui") != null) {
         ConfigContainer container = this.getConfigContainer();
         if (container instanceof ClientModule) {
            System.out.println("[HachimiKey] enabled " + ((ClientModule)container).OO() + " = " + flag);
            StackTraceElement[] trace = new Throwable().getStackTrace();
            for (int i = 1; i < Math.min(trace.length, 6); i++) {
               System.out.println("    at " + trace[i]);
            }
         }
      }
      super.Kl(flag);
      ConfigContainer configContainer = this.getConfigContainer();
      if (configContainer instanceof ClientModule) {
         if (flag) {
            EventBusImpl.INSTANCE.rD((ClientModule)configContainer);
         } else {
            EventBusImpl.INSTANCE.rG((ClientModule)configContainer);
         }
      }
   }

   public void NR() {
      ConfigContainer configContainer = this.getConfigContainer();
      if (configContainer instanceof ClientModule) {
         ((ClientModule)configContainer).awb();
      }
   }

   public void NS() {
      ConfigContainer configContainer = this.getConfigContainer();
      if (configContainer instanceof ClientModule) {
         ((ClientModule)configContainer).awc();
      }
   }

   @Override
   public void aty(Object obj) {
      this.Kl((Boolean)obj);
   }
}
