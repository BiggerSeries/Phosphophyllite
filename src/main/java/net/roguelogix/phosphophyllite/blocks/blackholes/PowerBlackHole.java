package net.roguelogix.phosphophyllite.blocks.blackholes;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.phosphophyllite.blocks.whiteholes.PowerWhiteHoleTile;
import net.roguelogix.phosphophyllite.debug.IDebuggable;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerBlackHole extends PhosphophylliteBlock implements EntityBlock {
    
    @RegisterBlock(name = "power_black_hole", tileEntityClass = PowerBlackHoleTile.class)
    public static final PowerBlackHole INSTANCE = new PowerBlackHole();
    
    public PowerBlackHole() {
        super(Properties.of());
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return PowerBlackHoleTile.SUPPLIER.create(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
        return (level, pos, state, entity) -> {
            assert entity instanceof PowerBlackHoleTile;
            ((PowerBlackHoleTile) entity).tick();
        };
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (Util.isWrench(player.getMainHandItem().getItem())) {
                if (level.getBlockEntity(pos) instanceof PowerBlackHoleTile tile) {
                    if (player.isCrouching()) {
                        tile.rotateCapability(player);
                    } else {
                        tile.nextOption(player);
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (player.getMainHandItem().isEmpty() && !player.isLocalPlayer()) {
                if (level.getBlockEntity(pos) instanceof IDebuggable debuggable) {
                    player.sendSystemMessage(Component.literal(Objects.requireNonNull(debuggable.getDebugInfo()).toString()));
                }
            }
        }
        return InteractionResult.PASS;
    }
}
