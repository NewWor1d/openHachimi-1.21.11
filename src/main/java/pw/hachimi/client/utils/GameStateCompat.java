package pw.hachimi.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ProgressScreen;

/**
 * 1.21.11 删除了 {@code DownloadingTerrainScreen}（进入世界时的地形下载界面），
 * 原代码用 {@code currentScreen instanceof DownloadingTerrainScreen} 判断
 * "还在加载地形，先不要执行模块逻辑"。
 *
 * TODO 1.21.11: 新版本的地形加载由 Overlay / 渲染线程接管，没有同名界面类。
 * 这里用 ProgressScreen（连接服务器阶段）作为近似，进入世界后的短暂加载窗口
 * 不再被拦截。
 */
public final class GameStateCompat {
   private GameStateCompat() {
   }

   public static boolean isDownloadingTerrain() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.currentScreen instanceof ProgressScreen;
   }
}
