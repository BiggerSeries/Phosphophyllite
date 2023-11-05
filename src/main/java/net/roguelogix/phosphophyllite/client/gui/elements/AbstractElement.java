package net.roguelogix.phosphophyllite.client.gui.elements;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.ScreenCallbacks;

import javax.annotation.Nonnull;

/**
 * Basic screen element, used by all other elements in Phosphophyllite's screen system.
 * Custom elements, at some point, must extend this class, or they may not be renderable in a {@link PhosphophylliteScreen PhosphophylliteScreen}.
 *
 * @param <T> Elements must be parented to a screen implementing {@link net.minecraft.world.inventory.AbstractContainerMenu AbstractContainerMenu}.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractElement<T extends AbstractContainerMenu> implements Tickable, GuiEventListener {

    /**
     * The parent screen of this element.
     */
    protected PhosphophylliteScreen<T> parent;

    /**
     * The position of this element.
     */
    public int x, y;

    /**
     * The dimensions of this element.
     */
    public int width, height;

    /**
     * Used to enable or disable the element entirely.
     * This is a generic state toggle and can be used for whatever.
     * TODO: remove this, migrate logic to whatever uses it.
     */
    public boolean stateEnable;

    /**
     * Callback for ticks/updates.
     */
    public ScreenCallbacks.OnTick onTick;

    /**
     * Default constructor.
     *
     * @param parent The parent screen of this element.
     * @param x      The x position of this element.
     * @param y      The y position of this element.
     * @param width  The width of this element.
     * @param height The height of this element.
     */
    public AbstractElement(@Nonnull PhosphophylliteScreen<T> parent, int x, int y, int width, int height) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Tick/update this element.
     */
    @Override
    public void tick() {
        // Check conditions, and trigger.
        if (this.onTick != null) {
            this.onTick.trigger();
        }
    }

    /**
     * Enable all "config" booleans for this element.
     * What this does is entirely up to the implementing element.
     */
    public abstract void enable();

    /**
     * Disable all "config" booleans for this element.
     * What this does is entirely up to the implementing element.
     */
    public abstract void disable();

    /**
     * Returns whether the mouse is over the current element or not.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @return True if the mouse is over this element, false otherwise.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Get actual x and y positions.
        int relativeX = this.parent.getGuiLeft() + this.x;
        int relativeY = this.parent.getGuiTop() + this.y;
        // Check the mouse.
        return ((mouseX > relativeX) && (mouseX < relativeX + this.width)
                && (mouseY > relativeY) && (mouseY < relativeY + this.height));
    }
}
