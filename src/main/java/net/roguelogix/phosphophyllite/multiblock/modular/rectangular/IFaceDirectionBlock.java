package net.roguelogix.phosphophyllite.multiblock.modular.rectangular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.roguelogix.phosphophyllite.modular.api.BlockModule;
import net.roguelogix.phosphophyllite.modular.api.IModularBlock;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IFaceDirectionBlock extends IModularBlock {
    class Module extends BlockModule<IFaceDirectionBlock> {
    
        public Module(IFaceDirectionBlock iface) {
            super(iface);
        }
    
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStates.FACING);
        }
    
        @Override
        public BlockState buildDefaultState(BlockState state) {
            return state.setValue(BlockStates.FACING, Direction.UP);
        }
    
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IFaceDirectionBlock.class, Module::new);
        }
    }
}
