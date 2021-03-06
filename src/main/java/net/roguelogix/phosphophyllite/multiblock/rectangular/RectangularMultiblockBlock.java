package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;

import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.*;

public abstract class RectangularMultiblockBlock<ControllerType extends RectangularMultiblockController<ControllerType, TileType, BlockType>, TileType extends RectangularMultiblockTile<ControllerType, TileType, BlockType>, BlockType extends RectangularMultiblockBlock<ControllerType, TileType, BlockType>> extends MultiblockBlock<ControllerType, TileType, BlockType> {
    
    public RectangularMultiblockBlock(@Nonnull Properties properties) {
        super(properties);
        if (usesAxisPositions()) {
            setDefaultState(getDefaultState().with(X_AXIS_POSITION, MIDDLE));
            setDefaultState(getDefaultState().with(Y_AXIS_POSITION, MIDDLE));
            setDefaultState(getDefaultState().with(Z_AXIS_POSITION, MIDDLE));
        }
        if (usesFaceDirection()){
            setDefaultState(getDefaultState().with(BlockStates.FACING, Direction.UP));
        }
    }
    
    @Override
    protected void fillStateContainer(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
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
