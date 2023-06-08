package net.roguelogix.phosphophyllite.client.gui.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.api.ITooltip;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A non-rendered screen element, used for adding tooltips. Useful if you have a static symbol baked into the texture already,
 * but still want a tooltip to display.
 *
 * @param <T> Elements must be parented to a screen implementing {@link net.minecraft.world.inventory.AbstractContainerMenu AbstractContainerMenu}.
 */
@OnlyIn(Dist.CLIENT)
public class TooltipElement<T extends AbstractContainerMenu> extends AbstractElement<T> implements ITooltip {

    /**
     * Used to enable or disable the tooltip.
     */
    public boolean tooltipEnable;

    /**
     * The tooltip to display. If null, a tooltip will not render.
     */
    public Component tooltip;

    /**
     * Default constructor.
     *
     * @param parent  The parent screen of this element.
     * @param x       The x position of this element.
     * @param y       The y position of this element.
     * @param width   The width of this element.
     * @param height  The height of this element.
     * @param tooltip The tooltip to display. If null, a tooltip will not render.
     */
    public TooltipElement(@Nonnull PhosphophylliteScreen<T> parent, int x, int y, int width, int height, @Nullable Component tooltip) {
        super(parent, x, y, width, height);
        this.tooltipEnable = (tooltip != null);
        this.tooltip = tooltip;
    }

    /**
     * Render tooltip.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    @Override
    public void renderTooltip(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Check conditions, and render tooltip.
        if (this.tooltipEnable && this.tooltip != null && this.isMouseOver(mouseX, mouseY)) {
            final List<FormattedCharSequence> list = Arrays.stream(tooltip.getString().split("\\n")).map(Component::literal).map(MutableComponent::getVisualOrderText).collect(Collectors.toList());
            graphics.renderTooltip(this.parent.getFont(), list, mouseX, mouseY);
        }
    }

    /**
     * Enable rendering of the tooltip.
     */
    @Override
    public void enable() {
        this.tooltipEnable = true;
    }

    /**
     * Disable rendering of the tooltip.
     */
    @Override
    public void disable() {
        this.tooltipEnable = false;
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