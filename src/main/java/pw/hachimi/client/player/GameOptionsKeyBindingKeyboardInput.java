package pw.hachimi.client.player;

import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.Vec2f;
import pw.hachimi.client.module.player.Freecam;

public class GameOptionsKeyBindingKeyboardInput extends KeyboardInput {
   public final GameOptions gameOptions;
   final Freecam freecam;
   public static boolean a;

   public GameOptionsKeyBindingKeyboardInput(Freecam freecam2, GameOptions gameOptions2) {
      super(gameOptions2);
      this.freecam = freecam2;
      this.gameOptions = gameOptions2;
   }

   @Override
   public void tick() {
      if (this.freecam.e) {
         super.tick();
      } else {
         this.ev();
         float f = (Float)this.freecam.speed.atx() / 10.0F;
         float f2 = this.freecam.hc(this.gameOptions.forwardKey.isPressed(), this.gameOptions.backKey.isPressed());
         float f3 = this.freecam.hc(this.gameOptions.leftKey.isPressed(), this.gameOptions.rightKey.isPressed());
         Vec2f vec2f = this.freecam.hd(f, f2, f3);
         float f4 = 0.0F;
         if (this.gameOptions.jumpKey.isPressed()) {
            f4 = 0.0F + f;
         } else if (this.gameOptions.sneakKey.isPressed()) {
            f4 = 0.0F - f;
         }

         this.freecam.vec2 = this.freecam.vec;
         double temp = vec2f.x;
         double temp2 = f4;
         double d = vec2f.y;
         double d2 = temp2;
         double d3 = temp;
         this.freecam.vec = this.freecam.vec.add(d3, d2, d);
      }
   }

   public void ev() {
      // 1.21.11: 输入状态统一收纳进 PlayerInput 记录，movementVector 归零
      this.playerInput = net.minecraft.util.PlayerInput.DEFAULT;
      this.movementVector = Vec2f.ZERO;
   }
}
