package net.roguelogix.phosphophyllite.multiblock.modular;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public interface ICoreMultiblockTileModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType>
        > {
    
    default boolean shouldConnectTo(TileType tile, Direction direction) {
        return true;
    }
    
    default void aboutToAttemptAttach() {
    }
    
    default void aboutToUnloadDetach() {
    }
    
    default void aboutToRemovedDetach() {
    }
    
    default void onControllerChange() {
    }
}
