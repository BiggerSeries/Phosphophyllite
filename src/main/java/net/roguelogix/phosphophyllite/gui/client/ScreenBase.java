package net.roguelogix.phosphophyllite.gui.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.gui.client.api.IRender;
import net.roguelogix.phosphophyllite.gui.client.api.ITooltip;
import net.roguelogix.phosphophyllite.gui.client.elements.AbstractElement;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Base screen.
 *
 * @param <T> Screens must belong to a Container.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("rawtypes")
public class ScreenBase<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements MenuAccess<T> {

    /**
     * The list of elements that are a part of this screen.
     */
    private final List<AbstractElement> screenElements;

    /**
     * The texture map this screen accesses.
     */
    protected ResourceLocation textureAtlas;
    
    /**
     * Player inventory
     */
    protected Inventory inventory;

    /**
     * This constructor makes no assumptions.
     */
    public ScreenBase(T screenContainer, Inventory playerInventory, Component title, ResourceLocation textureAtlas, int width, int height) {
        super(screenContainer, playerInventory, title);
        this.inventory = playerInventory;
        this.textureAtlas = textureAtlas;
        this.imageWidth = width;
        this.imageHeight = height;
        this.screenElements = Lists.newArrayList();
    }

    /**
     * Register a screen element with this screen.
     *
     * @param element The element to register.
     */
    public void addElement(AbstractElement element) {
        if (element != null) {
            this.screenElements.add(element);
        }
        //this.addListener(element);
    }

    /**
     * Get the width of the screen.
     *
     * @return The width.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the screen.
     *
     * @return The height.
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Get the font renderer for this screen.
     *
     * @return The font renderer.
     */
    public Font getFont() {
        return this.font;
    }

    /**
     * Update the current texture atlas.
     *
     * @param textureAtlas The atlas to switch to.
     */
    public void setTextureAtlas(ResourceLocation textureAtlas) {
        this.textureAtlas = textureAtlas;
    }

    /**
     * Draw the screen.
     *
     * @param mStack       The current matrix stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(@Nonnull PoseStack mStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(mStack);
        super.render(mStack, mouseX, mouseY, partialTicks);

        // Draw tooltips for all of the elements that belong to this screen.
        this.renderTooltip(mStack, mouseX, mouseY);
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and render.
            if (element instanceof ITooltip) {
                ((ITooltip) element).renderTooltip(mStack, mouseX, mouseY);
            }
        }
    }

    /**
     * Draw the foreground.
     *
     * @param mStack The current matrix stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    protected void renderLabels(@Nonnull PoseStack mStack, int mouseX, int mouseY) {
        // Bind to the correct texture & reset render color.
        RenderHelper.bindTexture(this.textureAtlas);
        RenderHelper.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw all of the elements that belong to this screen.
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and render.
            if (element instanceof IRender) {
                ((IRender) element).render(mStack, mouseX, mouseY);
            }
        }

        // Draw title.
        this.font.draw(mStack, this.title.getString(), this.titleLabelX, this.titleLabelY, 4210752);
    }

    /**
     * Draw the background.
     *
     * @param mStack       The current matrix stack.
     * @param partialTicks Partial ticks.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     */
    @Override
    protected void renderBg(@Nonnull PoseStack mStack, float partialTicks, int mouseX, int mouseY) {
        // Bind to the correct texture & reset render color.
        RenderHelper.bindTexture(this.textureAtlas);
        RenderHelper.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw background.
        this.blit(mStack, this.getGuiLeft(), this.getGuiTop(), 0, 0, this.getXSize(), this.getYSize());
    }
    
    /**
     * Returns whether the mouse is over the desired area.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param areaX  The x position of the area you want to check for.
     * @param areaY  The y position of the area you want to check for.
     * @param areaWidth  The width of the area to check.
     * @param areaHeight The height of the area to check.
     * @return True if the mouse is over the desired area, false otherwise.
     */
    public boolean isMouseOver(double mouseX, double mouseY, double areaX, double areaY, int areaWidth, int areaHeight) {
        // Get actual x and y positions.
        int relativeX = (int) (this.getGuiLeft() + areaX);
        int relativeY = (int) (this.getGuiTop() + areaY);
        // Check the mouse.
        return ((mouseX > relativeX) && (mouseX < relativeX + areaWidth)
                && (mouseY > relativeY) && (mouseY < relativeY + areaHeight));
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Iterate through this screen's elements.
        for (AbstractElement element : this.screenElements) {
            // Trigger.
            ((Tickable) element).tick();
        }
    }

    /**
     * Triggered when the mouse is moved.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Iterate through this screen's elements.
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                element.mouseMoved(mouseX, mouseY);
            }
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
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseClicked(mouseX, mouseY, button));
            }
        }
        return (handled || super.mouseClicked(mouseX, mouseY, button));
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
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseReleased(mouseX, mouseY, button));
            }
        }
        return (handled || super.mouseReleased(mouseX, mouseY, button));
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
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseDragged(mouseX, mouseY, button, dragX, dragY));
            }
        }
        return (handled || super.mouseDragged(mouseX, mouseY, button, dragX, dragY));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseScrolled(mouseX, mouseY, delta));
            }
        }
        return (handled || super.mouseScrolled(mouseX, mouseY, delta));
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
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.keyPressed(keyCode, scanCode, modifiers));
            }
        }
        // Return either the handle status, or the parent's return.
        return (handled || super.keyPressed(keyCode, scanCode, modifiers));
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
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.keyReleased(keyCode, scanCode, modifiers));
            }
        }
        return (handled || super.keyReleased(keyCode, scanCode, modifiers));
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
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.charTyped(codePoint, modifiers));
            }
        }
        return (handled || super.charTyped(codePoint, modifiers));
    }
}
