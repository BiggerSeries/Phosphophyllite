package net.roguelogix.phosphophyllite.client.gui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Tooltip interface.
 */
@Deprecated
@OnlyIn(Dist.CLIENT)
public interface ITooltip {

    /**
     * Render tooltip.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    void renderTooltip(@Nonnull PoseStack poseStack, int mouseX, int mouseY);
}
