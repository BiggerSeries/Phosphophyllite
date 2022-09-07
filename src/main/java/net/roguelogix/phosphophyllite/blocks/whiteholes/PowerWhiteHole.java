package net.roguelogix.phosphophyllite.blocks.whiteholes;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerWhiteHole extends PhosphophylliteBlock implements EntityBlock {
    
    @RegisterBlock(name = "power_white_hole", tileEntityClass = PowerWhiteHoleTile.class)
    public static final PowerWhiteHole INSTANCE = new PowerWhiteHole();
    
    public PowerWhiteHole() {
        super(Properties.of(Material.METAL));
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
        return (level, pos, state, entity) -> {
            assert entity instanceof PowerWhiteHoleTile;
            ((PowerWhiteHoleTile) entity).tick();
        };
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return PowerWhiteHoleTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        var tile = level.getBlockEntity(pos);
        if (tile instanceof PowerWhiteHoleTile whiteHoleTile) {
            whiteHoleTile.updateCapability(Util.directionFromPositions(pos, fromPos), blockIn, fromPos);
        }
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (Util.isWrench(player.getMainHandItem().getItem())) {
                if (level.getBlockEntity(pos) instanceof PowerWhiteHoleTile tile) {
                    if (player.isCrouching()) {
                        tile.rotateCapability(player);
                    } else {
                        tile.nextOption(player);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
