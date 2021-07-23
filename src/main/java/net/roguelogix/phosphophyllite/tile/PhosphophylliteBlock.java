package net.roguelogix.phosphophyllite.tile;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteBlock extends Block implements EntityBlock {
    
    private final TileSupplier<PhosphophylliteTile> tileConstructor;
    
    public PhosphophylliteBlock(TileSupplier<PhosphophylliteTile> tileConstructor, Properties properties) {
        super(properties);
        this.tileConstructor = tileConstructor;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (!(tile instanceof PhosphophylliteTile)) {
            throw new IllegalStateException("PhosphophylliteBlock must have a PhosphophylliteTile");
        }
        ((PhosphophylliteTile) tile).onBlockUpdate(fromPos);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tileConstructor.create(pos, state);
    }
}
