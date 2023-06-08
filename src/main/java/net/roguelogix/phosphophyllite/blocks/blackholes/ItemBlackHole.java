package net.roguelogix.phosphophyllite.blocks.blackholes;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemBlackHole extends Block implements EntityBlock {
    
    @RegisterBlock(name = "item_black_hole", tileEntityClass = ItemBlackHoleTile.class)
    public static final ItemBlackHole INSTANCE = new ItemBlackHole();
    
    public ItemBlackHole() {
        super(Properties.of());
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ItemBlackHoleTile.SUPPLIER.create(pos, state);
    }
}
