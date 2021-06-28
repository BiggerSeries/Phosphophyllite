package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockTile;
import net.roguelogix.phosphophyllite.util.BlockStates;

import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.*;
import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.Z_AXIS_POSITION;

public abstract class RectangularMultiblockTile<ControllerType extends RectangularMultiblockController<ControllerType, TileType, BlockType>, TileType extends RectangularMultiblockTile<ControllerType, TileType, BlockType>, BlockType extends RectangularMultiblockBlock<ControllerType, TileType, BlockType>> extends MultiblockTile<ControllerType, TileType, BlockType> {
    
    public RectangularMultiblockTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    
    @Override
    protected BlockState assembledBlockState() {
        BlockState state = super.assembledBlockState();
        @SuppressWarnings("unchecked") BlockType block = (BlockType) getBlockState().getBlock();
        if (block.usesAxisPositions()) {
            BlockPos pos = getPos();
            
            if (pos.getX() == controller.minCoord().x()) {
                state = state.with(X_AXIS_POSITION, AxisPosition.LOWER);
            } else if (pos.getX() == controller.maxCoord().x()) {
                state = state.with(X_AXIS_POSITION, AxisPosition.UPPER);
            } else {
                state = state.with(X_AXIS_POSITION, AxisPosition.MIDDLE);
            }
            
            if (pos.getY() == controller.minCoord().y()) {
                state = state.with(Y_AXIS_POSITION, AxisPosition.LOWER);
            } else if (pos.getY() == controller.maxCoord().y()) {
                state = state.with(Y_AXIS_POSITION, AxisPosition.UPPER);
            } else {
                state = state.with(Y_AXIS_POSITION, AxisPosition.MIDDLE);
            }
            
            if (pos.getZ() == controller.minCoord().z()) {
                state = state.with(Z_AXIS_POSITION, AxisPosition.LOWER);
            } else if (pos.getZ() == controller.maxCoord().z()) {
                state = state.with(Z_AXIS_POSITION, AxisPosition.UPPER);
            } else {
                state = state.with(Z_AXIS_POSITION, AxisPosition.MIDDLE);
            }
        }
        if (block.usesFaceDirection()) {
            BlockPos pos = getPos();
            if (pos.getX() == controller.minCoord().x()) {
                state = state.with(BlockStates.FACING, Direction.WEST);
            } else if (pos.getX() == controller.maxCoord().x()) {
                state = state.with(BlockStates.FACING, Direction.EAST);
            } else if (pos.getY() == controller.minCoord().y()) {
                state = state.with(BlockStates.FACING, Direction.DOWN);
            } else if (pos.getY() == controller.maxCoord().y()) {
                state = state.with(BlockStates.FACING, Direction.UP);
            } else if (pos.getZ() == controller.minCoord().z()) {
                state = state.with(BlockStates.FACING, Direction.NORTH);
            } else if (pos.getZ() == controller.maxCoord().z()) {
                state = state.with(BlockStates.FACING, Direction.SOUTH);
            }
        }
        return state;
    }
}
