package pw.hachimi.client.mixin;

import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Retained as a compatibility target; legacy ItemRenderer accessors were removed in 1.21.11. */
@Mixin(ItemRenderer.class)
public interface ItemRendererMixin {
}
