package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockTile;

public abstract class RectangularMultiblockTile<ControllerType extends RectangularMultiblockController<ControllerType, TileType, BlockType>, TileType extends RectangularMultiblockTile<ControllerType, TileType, BlockType>, BlockType extends RectangularMultiblockBlock<ControllerType, TileType, BlockType>> extends MultiblockTile<ControllerType, TileType, BlockType> {
    
    public RectangularMultiblockTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
}
