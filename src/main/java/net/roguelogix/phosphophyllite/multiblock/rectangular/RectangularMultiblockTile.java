package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockTile;
import net.roguelogix.phosphophyllite.util.BlockStates;

import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.*;
import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.Z_AXIS_POSITION;

public abstract class RectangularMultiblockTile<ControllerType extends RectangularMultiblockController<ControllerType, TileType, BlockType>, TileType extends RectangularMultiblockTile<ControllerType, TileType, BlockType>, BlockType extends RectangularMultiblockBlock<ControllerType, TileType, BlockType>> extends MultiblockTile<ControllerType, TileType, BlockType> {
    
    public RectangularMultiblockTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    protected BlockState assembledBlockState() {
        BlockState state = super.assembledBlockState();
        @SuppressWarnings("unchecked") BlockType block = (BlockType) getBlockState().getBlock();
        if (block.usesAxisPositions()) {
            BlockPos pos = getBlockPos();
            
            if (pos.getX() == controller.minCoord().x()) {
                state = state.setValue(X_AXIS_POSITION, AxisPosition.LOWER);
            } else if (pos.getX() == controller.maxCoord().x()) {
                state = state.setValue(X_AXIS_POSITION, AxisPosition.UPPER);
            } else {
                state = state.setValue(X_AXIS_POSITION, AxisPosition.MIDDLE);
            }
            
            if (pos.getY() == controller.minCoord().y()) {
                state = state.setValue(Y_AXIS_POSITION, AxisPosition.LOWER);
            } else if (pos.getY() == controller.maxCoord().y()) {
                state = state.setValue(Y_AXIS_POSITION, AxisPosition.UPPER);
            } else {
                state = state.setValue(Y_AXIS_POSITION, AxisPosition.MIDDLE);
            }
            
            if (pos.getZ() == controller.minCoord().z()) {
                state = state.setValue(Z_AXIS_POSITION, AxisPosition.LOWER);
            } else if (pos.getZ() == controller.maxCoord().z()) {
                state = state.setValue(Z_AXIS_POSITION, AxisPosition.UPPER);
            } else {
                state = state.setValue(Z_AXIS_POSITION, AxisPosition.MIDDLE);
            }
        }
        if (block.usesFaceDirection()) {
            BlockPos pos = getBlockPos();
            if (pos.getX() == controller.minCoord().x()) {
                state = state.setValue(BlockStates.FACING, Direction.WEST);
            } else if (pos.getX() == controller.maxCoord().x()) {
                state = state.setValue(BlockStates.FACING, Direction.EAST);
            } else if (pos.getY() == controller.minCoord().y()) {
                state = state.setValue(BlockStates.FACING, Direction.DOWN);
            } else if (pos.getY() == controller.maxCoord().y()) {
                state = state.setValue(BlockStates.FACING, Direction.UP);
            } else if (pos.getZ() == controller.minCoord().z()) {
                state = state.setValue(BlockStates.FACING, Direction.NORTH);
            } else if (pos.getZ() == controller.maxCoord().z()) {
                state = state.setValue(BlockStates.FACING, Direction.SOUTH);
            }
        }
        return state;
    }
}
