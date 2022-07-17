package net.roguelogix.phosphophyllite.multiblock2.modular;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.ValidationException;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@NonnullDefault
public abstract class MultiblockControllerModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, ControllerType>,
        ControllerType extends MultiblockController<TileType, ControllerType>
        > {
    
    public final ControllerType controller;
    
    public MultiblockControllerModule(IModularMultiblockController<TileType, ControllerType> controller) {
        //noinspection unchecked
        this.controller = (ControllerType) controller;
    }
    
    public void postModuleConstruction() {
    }
    
    public boolean canAttachPart(TileType tile) {
        return true;
    }
    
    public void onPartAdded(@Nonnull TileType tile) {
    }
    
    public void onPartRemoved(@Nonnull TileType tile) {
    }
    
    public void onPartLoaded(TileType tile) {
    }
    
    public void onPartUnloaded(TileType tile) {
    }
    
    public void onPartAttached(TileType tile) {
    }
    
    public void onPartDetached(TileType tile) {
    }
    
    public void onPartPlaced(TileType tile) {
    }
    
    public void onPartBroken(TileType tile) {
    }
    
    public void merge(ControllerType other) {
    }
    
    public void split(List<ControllerType> others) {
    }
    
    public void onStateTransition(MultiblockController.AssemblyState oldAssemblyState, MultiblockController.AssemblyState newAssemblyState) {
    }
    
    public void preTick() {
    }
    
    public void postTick() {
    }
    
    public void preValidate() throws ValidationException {
    }
    
    public void validate() throws ValidationException {
    }
}
