package net.roguelogix.phosphophyllite.client.gui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
    void renderTooltip(@Nonnull GuiGraphics graphics, int mouseX, int mouseY);
}
