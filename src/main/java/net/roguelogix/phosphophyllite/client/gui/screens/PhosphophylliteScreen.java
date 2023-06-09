package net.roguelogix.phosphophyllite.client.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.api.IRender;
import net.roguelogix.phosphophyllite.client.gui.api.ITooltip;
import net.roguelogix.phosphophyllite.client.gui.elements.AbstractElement;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Basic screen. This may be used to build your own GUIs for mods, or can be safely ignored if you'd rather roll your own.
 * This abstraction also assumes you are working with GUIs that have an associated container. If not, then you are advised
 * to use the vanilla system for containerless GUIs (which, for the most part, is pretty alright).
 *
 * @param <T> Screens must have a screen container implementing {@link net.minecraft.world.inventory.AbstractContainerMenu AbstractContainerMenu}.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("rawtypes")
public class PhosphophylliteScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements MenuAccess<T> {

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
    public PhosphophylliteScreen(T screenContainer, Inventory playerInventory, Component title, ResourceLocation textureAtlas, int width, int height) {
        super(screenContainer, playerInventory, title);
        this.inventory = playerInventory;
        this.textureAtlas = textureAtlas;
        this.imageWidth = width;
        this.imageHeight = height;
        this.screenElements = Lists.newArrayList();
    }

    /**
     * Register an element with this screen.
     *
     * @param element The element to register.
     */
    public void addScreenElement(AbstractElement element) {
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
     * @param poseStack    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Draw tooltips for all the elements that belong to this screen.
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and render.
            if (element instanceof ITooltip) {
                ((ITooltip) element).renderTooltip(guiGraphics, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Draw the foreground.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    @Override
    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Bind to the correct texture & reset render color.
        RenderHelper.bindTexture(this.textureAtlas);
        RenderHelper.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw all the elements that belong to this screen.
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and render.
            if (element instanceof IRender) {
                ((IRender) element).render(guiGraphics, mouseX, mouseY);
            }
        }

        // Draw title.
        guiGraphics.drawString(this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    /**
     * Draw the background.
     *
     * @param poseStack    The current pose stack.
     * @param partialTicks Partial ticks.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Bind to the correct texture & reset render color.
        RenderHelper.bindTexture(this.textureAtlas);
        RenderHelper.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw background.
        guiGraphics.blit(this.textureAtlas, this.getGuiLeft(), this.getGuiTop(), 0, 0, this.getXSize(), this.getYSize());
    }

    /**
     * Returns whether the mouse is over the desired area.
     *
     * @param mouseX     The x position of the mouse.
     * @param mouseY     The y position of the mouse.
     * @param areaX      The x position of the area you want to check for.
     * @param areaY      The y position of the area you want to check for.
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
