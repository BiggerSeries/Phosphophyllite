package net.roguelogix.phosphophyllite.gui.client.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.roguelogix.phosphophyllite.gui.client.RenderHelper;
import net.roguelogix.phosphophyllite.gui.client.ScreenBase;
import net.roguelogix.phosphophyllite.gui.client.api.ICallback;
import net.roguelogix.phosphophyllite.gui.client.api.IRender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base symbol/renderable element.
 *
 * @param <T> Elements must belong to a Container or ContainerScreen.
 */
public class Symbol<T extends AbstractContainerMenu> extends Tooltip<T> implements IRender {

    /**
     * Used to enable or disable the rendering of this element.
     */
    public boolean renderEnable;

    /**
     * The texture offset to use when rendering this element (starting from the upper left, and moving to the lower right).
     */
    public int u, v;

    /**
     * Callback for custom rendering.
     */
    public ICallback.OnRender onRender;

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
     * @param tooltip The tooltip for this element. If null, a tooltip will not render. If you set a tooltip later, use StringTextComponent.EMPTY.
     */
    public Symbol(@Nonnull ScreenBase<T> parent, int x, int y, int width, int height, int u, int v, @Nullable Component tooltip) {
        super(parent, x, y, width, height, tooltip);
        this.u = u;
        this.v = v;
        this.renderEnable = true;
    }

    /**
     * Gets the parent screen's blit offset.
     *
     * @return The blit offset.
     */
    public int getBlitOffset() {
        return this.parent.getBlitOffset();
    }

    /**
     * Render element.
     *
     * @param mStack The current matrix stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void render(@Nonnull PoseStack mStack, int mouseX, int mouseY) {
        // Check conditions, and render.
        if (this.renderEnable) {
            if (this.onRender != null) {
                this.onRender.trigger(mStack, mouseX, mouseY);
            } else {
                this.blit(mStack);
            }
        }
        // Reset for next call.
        RenderHelper.clearRenderColor();
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param mStack The current matrix stack.
     * @see AbstractGui#blit(PoseStack, int, int, int, int, int, int)
     */
    public void blit(@Nonnull PoseStack mStack) {
        GuiComponent.blit(mStack, this.x, this.y, this.u, this.v, this.width, this.height, 256, 256);
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param mStack The current matrix stack.
     * @param u      The u offset in the current texture to draw.
     * @param v      The v offset in the current texture to draw.
     * @see AbstractGui#blit(PoseStack, int, int, int, int, int, int)
     */
    public void blit(@Nonnull PoseStack mStack, int u, int v) {
        GuiComponent.blit(mStack, this.x, this.y, u, v, this.width, this.height, 256, 256);
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param mStack The current matrix stack.
     * @param width  How wide to draw the element.
     * @param height How tall to draw the element.
     * @param u      The u offset in the current texture to draw.
     * @param v      The v offset in the current texture to draw.
     * @see AbstractGui#blit(PoseStack, int, int, int, int, int, int)
     */
    public void blit(@Nonnull PoseStack mStack, int width, int height, int u, int v) {
        GuiComponent.blit(mStack, this.x, this.y, u, v, width, height, 256, 256);
    }

    /**
     * Blit/draw a part of this element.
     *
     * @param mStack The current matrix stack.
     * @param x      The x position to draw at.
     * @param y      The y position to draw at.
     * @param u      The u offset in the current texture to draw.
     * @param v      The v offset in the current texture to draw.
     * @param width  How wide to draw the element.
     * @param height How tall to draw the element.
     * @see AbstractGui#blit(PoseStack, int, int, int, int, int, int)
     */
    public void blit(@Nonnull PoseStack mStack, int x, int y, int u, int v, int width, int height) {
        GuiComponent.blit(mStack, x, y, u, v, width, height, 256, 256);
    }

    /**
     * Enable all "config" booleans for this element, effectively making this element visible.
     */
    @Override
    public void enable() {
        super.enable();
        this.renderEnable = true;
    }

    /**
     * Disable all "config" booleans for this element, effectively making this element hidden.
     */
    @Override
    public void disable() {
        super.disable();
        this.renderEnable = false;
    }
}