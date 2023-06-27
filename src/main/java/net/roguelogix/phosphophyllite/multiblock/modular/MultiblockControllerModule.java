package net.roguelogix.phosphophyllite.multiblock.modular;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.debug.IDebuggable;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@NonnullDefault
public abstract class MultiblockControllerModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IModularMultiblockController<TileType, BlockType, ControllerType>
        > implements IDebuggable {
    
    public final ControllerType controller;
    
    public MultiblockControllerModule(ControllerType controller) {
        this.controller = controller;
    }
    
    public MultiblockControllerModule(IModularMultiblockController<TileType, BlockType, ControllerType> controller) {
        //noinspection unchecked
        this((ControllerType) controller);
    }
    
    public void postModuleConstruction() {
    }
    
    public List<MultiblockControllerModule<TileType, BlockType, ControllerType>> modules() {
        return controller.modules();
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
    
    public void update() {
    }
    
    @Nullable
    @Override
    public DebugInfo getDebugInfo() {
        return null;
    }
}
