package net.roguelogix.phosphophyllite.gui.client.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Tooltip interface.
 */
@OnlyIn(Dist.CLIENT)
public interface ITooltip {

    /**
     * Render tooltip.
     *
     * @param mStack The current matrix stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    void renderTooltip(@Nonnull PoseStack mStack, int mouseX, int mouseY);
}
