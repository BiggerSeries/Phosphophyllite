package net.roguelogix.phosphophyllite.client.gui.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.ScreenCallbacks;
import net.roguelogix.phosphophyllite.client.gui.api.IRender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A rendered screen element, used for displaying symbols. Useful if you have a symbol that changes over time, such as a
 * liquid indicator, a progress bar, or a fuel gauge.
 *
 * @param <T> Elements must be parented to a screen implementing {@link net.minecraft.world.inventory.AbstractContainerMenu AbstractContainerMenu}.
 */
@OnlyIn(Dist.CLIENT)
public class RenderedElement<T extends AbstractContainerMenu> extends TooltipElement<T> implements IRender {

    /**
     * Used to enable or disable the element.
     */
    public boolean renderEnable;

    /**
     * The texture offset to use when rendering this element (starting from the upper left, and moving to the lower right).
     */
    public int u, v;

    /**
     * Callback for custom rendering.
     */
    public ScreenCallbacks.OnRender onRender;

    /**
     * Default constructor.
     *
     * @param parent  The parent screen of this element.
     * @param x       The x position of this element.
     * @param y       The y position of this element.
     * @param width   The width of this element.
     * @param height  The height of this element.
     * @param u       The u offset to use when rendering this element (starting from the left, and moving right).
     * @param v       The v offset to use when rendering this element (starting from the top, and moving down).
     * @param tooltip The tooltip to display. If null, a tooltip will not render.
     */
    public RenderedElement(@Nonnull PhosphophylliteScreen<T> parent, int x, int y, int width, int height, int u, int v, @Nullable Component tooltip) {
        super(parent, x, y, width, height, tooltip);
        this.renderEnable = true;
        this.u = u;
        this.v = v;
    }

    /**
     * Gets the parent screen's blit offset.
     *
     * @return The blit offset.
     */
    public int getBlitOffset() {
        // TODO: figure out a proper replacement
//        return this.parent.getBlitOffset();
        return 0;
    }

    /**
     * Render the element.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Check conditions, and render.
        if (this.renderEnable) {
            if (this.onRender != null) {
                this.onRender.trigger(graphics, mouseX, mouseY);
            } else {
//                this.blit(graphics);
            }
        }
        // Reset for next call.
        RenderHelper.clearRenderColor();
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param graphics The current pose stack.
     * @see GuiGraphics#blit(PoseStack, int, int, int, int, int, int) GuiGraphics.blit(PoseStack, int, int, int, int, int, int)
     */
    public void blit(@Nonnull GuiGraphics graphics) {
        graphics.blit(RenderHelper.getCurrentResource(), this.x, this.y, this.u, this.v, this.width, this.height, 256, 256);
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param poseStack The current pose stack.
     * @param u         The u offset in the current texture to draw.
     * @param v         The v offset in the current texture to draw.
     * @see GuiGraphics#blit(PoseStack, int, int, float, float, int, int, int, int) GuiGraphics.blit(PoseStack, int, int, float, float, int, int, int, int)
     */
    public void blit(@Nonnull GuiGraphics graphics, int u, int v) {
        graphics.blit(RenderHelper.getCurrentResource(), this.x, this.y, u, v, this.width, this.height, 256, 256);
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param poseStack The current pose stack.
     * @param width     How wide to draw the element.
     * @param height    How tall to draw the element.
     * @param u         The u offset in the current texture to draw.
     * @param v         The v offset in the current texture to draw.
     * @see GuiGraphics#blit(PoseStack, int, int, float, float, int, int, int, int) GuiGraphics.blit(PoseStack, int, int, float, float, int, int, int, int)
     */
    public void blit(@Nonnull GuiGraphics graphics, int width, int height, int u, int v) {
        graphics.blit(RenderHelper.getCurrentResource(), this.x, this.y, u, v, width, height, 256, 256);
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param poseStack The current pose stack.
     * @param x         The x position to draw at.
     * @param y         The y position to draw at.
     * @param u         The u offset in the current texture to draw.
     * @param v         The v offset in the current texture to draw.
     * @param width     How wide to draw the element.
     * @param height    How tall to draw the element.
     * @see GuiGraphics#blit(PoseStack, int, int, float, float, int, int, int, int) GuiGraphics.blit(PoseStack, int, int, float, float, int, int, int, int)
     */
    public void blit(@Nonnull GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderHelper.getCurrentResource(), x, y, u, v, width, height, 256, 256);
    }

    /**
     * Enable rendering of the element, as well as any associated tooltips.
     */
    @Override
    public void enable() {
        super.enable();
        this.renderEnable = true;
    }

    /**
     * Disable rendering of the element, as well as any associated tooltips.
     */
    @Override
    public void disable() {
        super.disable();
        this.renderEnable = false;
    }
    
    boolean focused = false;
    
    @Override
    public void setFocused(boolean shouldFocus) {
        this.focused = shouldFocus;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
}