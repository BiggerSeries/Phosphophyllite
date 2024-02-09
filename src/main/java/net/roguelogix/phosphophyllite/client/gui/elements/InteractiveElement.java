package net.roguelogix.phosphophyllite.client.gui.elements;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.ScreenCallbacks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An interactive screen element, used for controlling logic. Useful if you have something that needs control, such as a
 * button or switch.
 *
 * @param <T> Elements must be parented to a screen implementing {@link net.minecraft.world.inventory.AbstractContainerMenu AbstractContainerMenu}.
 */
@OnlyIn(Dist.CLIENT)
public class InteractiveElement<T extends AbstractContainerMenu> extends RenderedElement<T> implements GuiEventListener {

    /**
     * Used to enable or disable interactions.
     */
    public boolean actionEnable;

    /**
     * Callback for mouse movement.
     */
    public ScreenCallbacks.OnMouseMoved onMouseMoved;

    /**
     * Callback for mouse button clicks.
     */
    public ScreenCallbacks.OnMouseClicked onMouseClicked;

    /**
     * Callback for mouse button releases.
     */
    public ScreenCallbacks.OnMouseReleased onMouseReleased;

    /**
     * Callback for mouse dragging.
     */
    public ScreenCallbacks.OnMouseDragged onMouseDragged;

    /**
     * Callback for mouse scrolling.
     */
    public ScreenCallbacks.OnMouseScrolled onMouseScrolled;

    /**
     * Callback for key presses.
     */
    public ScreenCallbacks.OnKeyPressed onKeyPressed;

    /**
     * Callback for key releases.
     */
    public ScreenCallbacks.OnKeyReleased onKeyReleased;

    /**
     * Callback for typed characters.
     */
    public ScreenCallbacks.OnCharTyped onCharTyped;

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
    public InteractiveElement(@Nonnull PhosphophylliteScreen<T> parent, int x, int y, int width, int height, int u, int v, @Nullable Component tooltip) {
        super(parent, x, y, width, height, u, v, tooltip);
        this.actionEnable = true;
    }
    
    /**
     * Play a sound.
     *
     * @param sound The sound to play.
     */
    public void playSound(Holder.Reference<SoundEvent> sound) {
        playSound(sound.value);
    }
    
    /**
     * Play a sound.
     *
     * @param sound The sound to play.
     */
    public void playSound(SoundEvent sound) {
        this.parent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    /**
     * Play a sound.
     *
     * @param sound  The sound to play.
     * @param volume How loud to play the sound.
     */
    public void playSound(SoundEvent sound, float volume) {
        this.parent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, volume));
    }


    /**
     * Triggered when the mouse is moved.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onMouseMoved != null) {
            this.onMouseMoved.trigger(mouseX, mouseY);
        }
    }

    /**
     * Triggered when the mouse is clicked.
     * For most purposes, it is recommended to tie logic to #mouseReleased.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onMouseClicked != null) {
            return this.onMouseClicked.trigger(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Triggered when the mouse is released.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onMouseReleased != null) {
            return this.onMouseReleased.trigger(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Triggered when the mouse is dragged.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @param dragX  Drag x.
     * @param dragY  Drag y.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onMouseDragged != null) {
            return this.onMouseDragged.trigger(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    /**
     * Triggered when the mouse is scrolled.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param delta  How far the mouse scrolled.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, double idfk) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onMouseScrolled != null) {
            return this.onMouseScrolled.trigger(mouseX, mouseY, delta);
        }
        return false;
    }

    /**
     * Triggered when a key is pressed.
     *
     * @param keyCode   The key code pressed.
     * @param scanCode  The scan code pressed.
     * @param modifiers Any modifiers pressed.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onKeyPressed != null) {
            return this.onKeyPressed.trigger(keyCode, scanCode, modifiers);
        }
        return false;
    }

    /**
     * Triggered when a key is released.
     *
     * @param keyCode   The key code released.
     * @param scanCode  The scan code released.
     * @param modifiers Any modifiers released.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onKeyReleased != null) {
            return this.onKeyReleased.trigger(keyCode, scanCode, modifiers);
        }
        return false;
    }

    /**
     * Triggered when a character is typed.
     *
     * @param codePoint The character typed.
     * @param modifiers Any modifiers released.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Check conditions, and trigger.
        if (this.actionEnable && this.onCharTyped != null) {
            return this.onCharTyped.trigger(codePoint, modifiers);
        }
        return false;
    }

    /**
     * Enable interactions, as well as any associated rendered elements or tooltips.
     */
    @Override
    public void enable() {
        super.enable();
        this.actionEnable = true;
    }

    /**
     * Disable interactions, as well as any associated rendered elements or tooltips.
     */
    @Override
    public void disable() {
        super.disable();
        this.actionEnable = false;
    }
}