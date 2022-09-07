package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.multiblock.MultiblockTileModule;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.*;
import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.Z_AXIS_POSITION;

@Deprecated(forRemoval = true)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RectangularMultiblockTileModule<
        TileType extends BlockEntity & IRectangularMultiblockTile<TileType, ControllerType>,
        ControllerType extends RectangularMultiblockController<TileType, ControllerType>
        > extends MultiblockTileModule<TileType, ControllerType> {
    
    private final boolean AXIS_POSITIONS = iface.getBlockState().hasProperty(X_AXIS_POSITION);
    private final boolean FACE_DIRECTION = iface.getBlockState().hasProperty(BlockStates.FACING);
    
    public RectangularMultiblockTileModule(IModularTile blockEntity) {
        super(blockEntity);
    }
    
    @Override
    protected BlockState assembledBlockState(BlockState state) {
        if (AXIS_POSITIONS) {
            BlockPos pos = iface.getBlockPos();
            
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
        if (FACE_DIRECTION) {
            BlockPos pos = iface.getBlockPos();
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
        return super.assembledBlockState(state);
    }
}
