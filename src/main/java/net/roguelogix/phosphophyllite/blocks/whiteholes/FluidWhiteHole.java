package net.roguelogix.phosphophyllite.blocks.whiteholes;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterBlock(name = "fluid_white_hole", tileEntityClass = FluidWhiteHoleTile.class)
public class FluidWhiteHole extends Block implements EntityBlock {
    
    @RegisterBlock.Instance
    public static FluidWhiteHole INSTANCE;
    
    public FluidWhiteHole() {
        super(Properties.of(Material.METAL));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidWhiteHoleTile(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
        return (level, pos, state, entity) -> {
            assert entity instanceof FluidWhiteHoleTile;
            ((FluidWhiteHoleTile) entity).tick();
        };
    }
    
    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        Item item = player.getMainHandItem().getItem();
        if(item instanceof BucketItem){
            var te = worldIn.getBlockEntity(pos);
            if(te instanceof FluidWhiteHoleTile){
                ((FluidWhiteHoleTile)te).setFluid(((BucketItem) item).getFluid());
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, worldIn, pos, player, handIn, hit);
    }
}
