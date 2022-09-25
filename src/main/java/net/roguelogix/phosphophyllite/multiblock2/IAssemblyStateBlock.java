package net.roguelogix.phosphophyllite.multiblock2;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.roguelogix.phosphophyllite.modular.api.BlockModule;
import net.roguelogix.phosphophyllite.modular.api.IModularBlock;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public interface IAssemblyStateBlock extends IModularBlock {
    
    BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    
    final class Module extends BlockModule<IAssemblyStateBlock> {
        private Module(IAssemblyStateBlock iface) {
            super(iface);
        }
        
        @Override
        public BlockState buildDefaultState(BlockState state) {
            state = state.setValue(ASSEMBLED, false);
            return state;
        }
        
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ASSEMBLED);
        }
        
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IAssemblyStateBlock.class, Module::new);
        }
    }
}
