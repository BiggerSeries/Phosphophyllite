package net.roguelogix.phosphophyllite.blocks.blackholes;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidBlackHole extends Block implements EntityBlock {
    
    @RegisterBlock(name = "fluid_black_hole", tileEntityClass = FluidBlackHoleTile.class)
    public static final FluidBlackHole INSTANCE = new FluidBlackHole();
    
    public FluidBlackHole() {
        super(Properties.of(Material.METAL));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidBlackHoleTile(pos, state);
    }
}
