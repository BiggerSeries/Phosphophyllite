package net.roguelogix.phosphophyllite.multiblock.validated;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.jetbrains.annotations.Contract;

import static net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock.ASSEMBLED;

@NonnullDefault
public interface IValidatedMultiblockTile<
        TileType extends BlockEntity & IValidatedMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IValidatedMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IValidatedMultiblock<TileType, BlockType, ControllerType>
        > extends IMultiblockTile<TileType, BlockType, ControllerType> {
    
    
    default Module<TileType, BlockType, ControllerType> validatedModule() {
        //noinspection unchecked,ConstantConditions
        return module(IValidatedMultiblockTile.class, Module.class);
    }
    
    class Module<TileType extends BlockEntity & IValidatedMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IValidatedMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IValidatedMultiblock<TileType, BlockType, ControllerType>
            > extends TileModule<TileType> {
        
        private final boolean ASSEMBLY_STATE = iface.getBlockState().hasProperty(ASSEMBLED);
        
        private final ObjectArrayList<IAssemblyStateTileModule> assemblyStateTileModules = new ObjectArrayList<>();
        
        @OnModLoad
        private static void onModLoad() {
            ModuleRegistry.registerTileModule(IValidatedMultiblockTile.class, Module::new);
        }
        
        public Module(IModularTile iface) {
            super(iface);
        }
        
        @Override
        public void postModuleConstruction() {
            for (final var module : iface.modules()) {
                if (module instanceof IAssemblyStateTileModule assemblyStateTileModule) {
                    assemblyStateTileModules.add(assemblyStateTileModule);
                }
            }
        }
        
        @Contract(pure = true)
        public BlockState assembledBlockState(BlockState state) {
            if (ASSEMBLY_STATE) {
                state = state.setValue(ASSEMBLED, true);
            }
            // TODO: this is going to have one hell of a performance impact
            for (var module : assemblyStateTileModules) {
                state = module.assembledBlockState(state);
            }
            return state;
        }
        
        @Contract(pure = true)
        public BlockState disassembledBlockState(BlockState state) {
            if (ASSEMBLY_STATE) {
                state = state.setValue(ASSEMBLED, false);
            }
            for (var module : assemblyStateTileModules) {
                state = module.disassembledBlockState(state);
            }
            return state;
        }
        
    }
}
