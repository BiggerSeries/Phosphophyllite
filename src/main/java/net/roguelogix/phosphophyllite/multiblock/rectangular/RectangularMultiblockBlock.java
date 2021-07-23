package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;

import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.*;

public abstract class RectangularMultiblockBlock<ControllerType extends RectangularMultiblockController<ControllerType, TileType, BlockType>, TileType extends RectangularMultiblockTile<ControllerType, TileType, BlockType>, BlockType extends RectangularMultiblockBlock<ControllerType, TileType, BlockType>> extends MultiblockBlock<ControllerType, TileType, BlockType> {
    
    public RectangularMultiblockBlock(@Nonnull Properties properties) {
        super(properties);
        if (usesAxisPositions()) {
            registerDefaultState(defaultBlockState().setValue(X_AXIS_POSITION, MIDDLE));
            registerDefaultState(defaultBlockState().setValue(Y_AXIS_POSITION, MIDDLE));
            registerDefaultState(defaultBlockState().setValue(Z_AXIS_POSITION, MIDDLE));
        }
        if (usesFaceDirection()){
            registerDefaultState(defaultBlockState().setValue(BlockStates.FACING, Direction.UP));
        }
    }
    
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (usesAxisPositions()) {
            builder.add(X_AXIS_POSITION);
            builder.add(Y_AXIS_POSITION);
            builder.add(Z_AXIS_POSITION);
        }
        if(usesFaceDirection()){
            builder.add(BlockStates.FACING);
        }
    }
    
    public boolean usesAxisPositions() {
        return false;
    }
    
    public boolean usesFaceDirection(){
        return false;
    }
    
    public abstract boolean isGoodForInterior();
    
    public abstract boolean isGoodForExterior();
    
    public abstract boolean isGoodForFrame();
    
    public boolean isGoodForCorner(){
        return isGoodForFrame();
    }
}
