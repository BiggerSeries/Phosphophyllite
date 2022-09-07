package net.roguelogix.phosphophyllite.multiblock;

@Deprecated(forRemoval = true)
public interface IOnAssemblyTile {
    /**
     * Called when a multiblock is first assembled or resumed from a paused state
     * called after a controller's onAssembled and blockstates are updated
     */
    void onAssembly();
}
