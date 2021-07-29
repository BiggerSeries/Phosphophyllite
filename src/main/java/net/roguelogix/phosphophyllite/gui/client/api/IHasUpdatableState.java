package net.roguelogix.phosphophyllite.gui.client.api;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IHasUpdatableState<T> {
    /**
     * @return The current state of the tile.
     */
    T getState();

    /**
     * Call for an update to the current state information.
     */
    void updateState();
}
