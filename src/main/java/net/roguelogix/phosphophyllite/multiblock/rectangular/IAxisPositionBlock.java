package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.roguelogix.phosphophyllite.modular.api.BlockModule;
import net.roguelogix.phosphophyllite.modular.api.IModularBlock;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.multiblock.rectangular.AxisPosition.*;

@Deprecated(forRemoval = true)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IAxisPositionBlock extends IModularBlock {
    final class Module extends BlockModule<IAxisPositionBlock> {
        
        public Module(IAxisPositionBlock iface) {
            super(iface);
        }
        
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(X_AXIS_POSITION);
            builder.add(Y_AXIS_POSITION);
            builder.add(Z_AXIS_POSITION);
        }
        
        @Override
        public BlockState buildDefaultState(BlockState state) {
            state = state.setValue(X_AXIS_POSITION, MIDDLE);
            state = state.setValue(Y_AXIS_POSITION, MIDDLE);
            state = state.setValue(Z_AXIS_POSITION, MIDDLE);
            return state;
        }
        
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IAxisPositionBlock.class, Module::new);
        }
    }
}
