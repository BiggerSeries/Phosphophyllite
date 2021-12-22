package net.roguelogix.phosphophyllite.client.gui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Render interface.
 */
@Deprecated
@OnlyIn(Dist.CLIENT)
public interface IRender {

    /**
     * Render
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    void render(PoseStack poseStack, int mouseX, int mouseY);
}
