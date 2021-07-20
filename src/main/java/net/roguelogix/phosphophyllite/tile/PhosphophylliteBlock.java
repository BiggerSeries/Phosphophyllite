package net.roguelogix.phosphophyllite.tile;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteBlock extends Block {
    
    private final Function<BlockState, PhosphophylliteTile> tileConstructor;
    
    public PhosphophylliteBlock(Function<BlockState, PhosphophylliteTile> tileConstructor, Properties properties) {
        super(properties);
        this.tileConstructor = tileConstructor;
    }
    
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        TileEntity tile = worldIn.getTileEntity(pos);
        if (!(tile instanceof PhosphophylliteTile)) {
            throw new IllegalStateException("PhosphophylliteBlock must have a PhosphophylliteTile");
        }
        ((PhosphophylliteTile) tile).onBlockUpdate(fromPos);
    }
    
    @Override
    public final boolean hasTileEntity(BlockState state) {
        return true;
    }
    
    @Nullable
    @Override
    public final TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return tileConstructor.apply(state);
    }
}
